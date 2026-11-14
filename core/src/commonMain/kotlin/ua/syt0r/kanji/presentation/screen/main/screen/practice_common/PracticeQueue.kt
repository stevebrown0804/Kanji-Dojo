package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.debounceFirst
import ua.syt0r.kanji.core.srs.SrsAnswers
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.srs.SrsScheduler
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.database.ReviewUndoToken
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days


interface PracticeQueue<State, Descriptor> {

    val state: StateFlow<State>

    suspend fun initialize(items: List<Descriptor>)
    suspend fun submitAnswer(answer: PracticeAnswer)
    fun immediateFinish()

}

interface PracticeQueueItem<T : PracticeQueueItem<T>> {

    val srsCardKey: SrsCardKey
    val srsCard: SrsCard
    val deckId: Long
    val repeats: Int
    val totalMistakes: Int
    val data: Deferred<Any>

    fun copyForRepeat(answer: PracticeAnswer): T

}

data class PracticeQueueProgress(
    val pending: Int,
    val repeats: Int,
    val completed: Int
)

interface PracticeSummaryItem {
    val totalReviews: Deferred<Int>
    val nextInterval: Duration
}

abstract class BasePracticeQueue<State, Descriptor, QueueItem, SummaryItem>(
    private val practiceScope: CoroutineScope,
    protected val timeUtils: TimeUtils,
    protected val srsScheduler: SrsScheduler,
    protected val srsCardRepository: SrsCardRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    analyticsManager: AnalyticsManager,
    private val enableReviewUndo: Boolean = false
) : PracticeQueue<State, Descriptor>
        where QueueItem : PracticeQueueItem<QueueItem>,
              SummaryItem : PracticeSummaryItem {

    protected open lateinit var queue: MutableList<QueueItem>
    protected val summaryItems = mutableMapOf<SrsCardKey, SummaryItem>()

    private data class UndoEntry<QueueItem, SummaryItem>(
        val item: QueueItem,
        val repeatPosition: Int?,
        val previousSummary: SummaryItem?,
        val token: ReviewUndoToken
    )

    private val undoHistory = ArrayDeque<UndoEntry<QueueItem, SummaryItem>>()
    protected val canUndo: Boolean get() = undoHistory.isNotEmpty()
    protected var undoCount = 0
        private set

    protected lateinit var practiceStartInstant: Instant
    private lateinit var currentReviewStartInstant: Instant

    private val submittedAnswersChannel = Channel<PracticeAnswer>()

    private val _state: MutableStateFlow<State> = MutableStateFlow(value = this.getLoadingState())
    override val state: StateFlow<State> = _state

    private val reviewReporter = PracticeReviewReporter(analyticsManager)

    init {
        submittedAnswersChannel.consumeAsFlow()
            .debounceFirst()
            .onEach { handleAnswer(it) }
            .launchIn(practiceScope)
    }

    protected abstract suspend fun Descriptor.toQueueItem(): QueueItem
    protected abstract fun createSummaryItem(
        queueItem: QueueItem,
        totalReviews: Deferred<Int>
    ): SummaryItem

    protected abstract fun getLoadingState(): State
    protected abstract suspend fun getReviewState(item: QueueItem, answers: PracticeAnswers): State
    protected abstract fun getSummaryState(): State

    override suspend fun initialize(items: List<Descriptor>) {
        practiceStartInstant = timeUtils.now()
        queue = items.map { it.toQueueItem() }.toMutableList()
        updateState()
    }

    override suspend fun submitAnswer(answer: PracticeAnswer) {
        if (enableReviewUndo) {
            handleUndoableAnswer(answer)
        } else {
            submittedAnswersChannel.send(answer)
        }
    }

    override fun immediateFinish() {
        undoHistory.clear()
        publishSummary()
    }

    private fun publishSummary() {
        val isLoading = summaryItems.any { it.value.totalReviews.isCompleted.not() }
        if (isLoading) {
            _state.value = getLoadingState()
            practiceScope.launch {
                summaryItems.forEach { it.value.totalReviews.await() }
                _state.value = getSummaryState()
            }
        } else _state.value = getSummaryState()
    }

    protected fun getProgress(): PracticeQueueProgress {
        return PracticeQueueProgress(
            pending = queue.count { it.repeats == 0 },
            repeats = queue.count { it.repeats > 0 },
            completed = summaryItems.filter { it.value.nextInterval >= 1.days }.size
        )
    }

    private fun getAnswers(answers: SrsAnswers): PracticeAnswers {
        return PracticeAnswers(
            again = PracticeAnswer(answers.again),
            hard = PracticeAnswer(answers.hard),
            good = PracticeAnswer(answers.good),
            easy = PracticeAnswer(answers.easy)
        )
    }

    private suspend fun handleAnswer(answer: PracticeAnswer) {
        val item = queue.removeFirstOrNull() ?: return
        val updatedItem = item.copyForRepeat(answer)

        saveSummaryData(updatedItem)

        val instant = timeUtils.now()
        val reviewDuration = instant - currentReviewStartInstant

        if (answer.srsAnswer.card.interval < 1.days) {
            placeItemBackToQueue(updatedItem)
        }

        updateState()

        srsCardRepository.update(item.srsCardKey, answer.srsAnswer.card)
        saveReviewHistory(item, answer, instant, reviewDuration)
        reviewReporter.reportReview(updatedItem, answer, reviewDuration)
    }

    private suspend fun handleUndoableAnswer(answer: PracticeAnswer) {
        val item = queue.firstOrNull() ?: return
        val updatedItem = item.copyForRepeat(answer)
        val instant = timeUtils.now()
        val reviewDuration = instant - currentReviewStartInstant
        val historyItem = reviewHistoryItem(item, answer, instant, reviewDuration)

        // Prepare the next card before saving so a loading failure leaves this grade unapplied.
        val repeatPosition = if (updatedItem.srsCard.interval < 1.days) {
            repeatPosition(updatedItem, queue.subList(1, queue.size))
        } else null
        val nextItem = if (repeatPosition == 0) updatedItem else queue.getOrNull(1)
        nextItem?.data?.await()

        withContext(NonCancellable) {
            val token = srsCardRepository.recordReview(item.srsCardKey, updatedItem.srsCard, historyItem)
            val summaryItem = createSummaryItem(updatedItem, CompletableDeferred(token.totalReviews))
            val previousSummary = summaryItems.put(item.srsCardKey, summaryItem)

            queue.removeFirst()
            if (repeatPosition != null) queue.add(repeatPosition, updatedItem)
            undoHistory.addLast(UndoEntry(item, repeatPosition, previousSummary, token))
        }

        updateState()
        reviewReporter.reportReview(updatedItem, answer, reviewDuration)
    }

    protected suspend fun undoAnswer(): Boolean {
        val entry = undoHistory.lastOrNull() ?: return false
        val restored = withContext(NonCancellable) {
            if (!srsCardRepository.undoReview(entry.token)) return@withContext false

            undoHistory.removeLast()
            if (entry.repeatPosition != null) queue.removeAt(entry.repeatPosition)
            queue.add(0, entry.item)
            if (entry.previousSummary == null) {
                summaryItems.remove(entry.item.srsCardKey)
            } else {
                summaryItems[entry.item.srsCardKey] = entry.previousSummary
            }
            undoCount++
            true
        }

        if (restored) updateState()
        return restored
    }

    private suspend fun updateState() {
        val item = queue.getOrNull(0)
        if (item == null) {
            publishSummary()
        } else {
            if (!item.data.isCompleted) {
                _state.value = getLoadingState()
            }
            val time = timeUtils.now()
            val srsAnswers = srsScheduler.answers(item.srsCard, time)

            item.data.await()
            currentReviewStartInstant = timeUtils.now()

            _state.value = getReviewState(item, getAnswers(srsAnswers))

            queue.getOrNull(1)?.apply {
                data.start()
            }
        }
    }

    private fun placeItemBackToQueue(
        updatedQueueItem: QueueItem
    ) {
        queue.add(repeatPosition(updatedQueueItem, queue), updatedQueueItem)
    }

    private fun repeatPosition(updatedQueueItem: QueueItem, remainingItems: List<QueueItem>): Int {
        val nextReviewTime = getExpectedReviewTime(updatedQueueItem.srsCard)
        return remainingItems.asSequence()
            .map { getExpectedReviewTime(it.srsCard) }
            .indexOfFirst { nextReviewTime < it }
            .takeIf { it != -1 }
            ?.let {
                if (it == 0 && remainingItems.isNotEmpty()) min(MIN_QUEUE_POSITION_SHIFT - 1, remainingItems.size)
                else min(it, MAX_QUEUE_POSITION_SHIFT - 1)
            }
            ?: min(remainingItems.size, MAX_QUEUE_POSITION_SHIFT - 1)
    }

    private fun getExpectedReviewTime(srsItem: SrsCard): Instant {
        return (srsItem.lastReview ?: Instant.DISTANT_PAST) + srsItem.interval
    }

    private fun saveSummaryData(queueItem: QueueItem) {
        val summaryItem = createSummaryItem(
            queueItem = queueItem,
            totalReviews = practiceScope.async {
                queueItem.srsCardKey.run {
                    reviewHistoryRepository.getTotalReviewCount(itemKey, practiceType)
                        .toInt()
                        .plus(1) // To count current review before its saved
                }
            }
        )
        summaryItems[queueItem.srsCardKey] = summaryItem
    }

    private suspend fun saveReviewHistory(
        queueItem: QueueItem,
        answer: PracticeAnswer,
        reviewStart: Instant,
        reviewDuration: Duration
    ) {
        reviewHistoryRepository.addReview(reviewHistoryItem(queueItem, answer, reviewStart, reviewDuration))
    }

    private fun reviewHistoryItem(
        queueItem: QueueItem,
        answer: PracticeAnswer,
        reviewStart: Instant,
        reviewDuration: Duration
    ): ReviewHistoryItem {
        return ReviewHistoryItem(
            key = queueItem.srsCardKey.itemKey,
            practiceType = queueItem.srsCardKey.practiceType,
            timestamp = reviewStart,
            duration = reviewDuration,
            grade = answer.srsAnswer.grade,
            mistakes = answer.mistakes,
            deckId = queueItem.deckId
        )
    }


    companion object {
        private const val MIN_QUEUE_POSITION_SHIFT = 3
        private const val MAX_QUEUE_POSITION_SHIFT = 10
    }

}

private class PracticeReviewReporter<T : PracticeQueueItem<*>>(
    private val analyticsManager: AnalyticsManager
) {

    fun reportReview(
        item: T,
        answer: PracticeAnswer,
        reviewDuration: Duration
    ) {
        analyticsManager.sendEvent("review") {
            put("key", item.srsCardKey.itemKey)
            put("practice_type", item.srsCardKey.practiceType)
            put("duration", reviewDuration.inWholeMilliseconds)
            put("mistakes", answer.mistakes)
            put("repeats", item.srsCard.fsrsCard.repeats)
            put("lapses", item.srsCard.fsrsCard.lapses)
        }
    }

}
