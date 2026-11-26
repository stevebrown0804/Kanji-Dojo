package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.srs.SrsScheduler
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BasePracticeQueue
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswers
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeQueue
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeItemData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueItem
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueItemDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeSummaryItem
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeQueueItemDataUseCase

interface MojiPracticeQueue : PracticeQueue<MojiPracticeQueueState, MojiPracticeQueueItemDescriptor> {
    suspend fun undoLastReview(): Boolean
}
typealias BaseMojiPracticeQueue = BasePracticeQueue<MojiPracticeQueueState, MojiPracticeQueueItemDescriptor, MojiPracticeQueueItem, MojiPracticeSummaryItem>

class DefaultMojiPracticeQueue(
    private val coroutineScope: CoroutineScope,
    timeUtils: TimeUtils,
    srsCardRepository: SrsCardRepository,
    reviewHistoryRepository: ReviewHistoryRepository,
    srsScheduler: SrsScheduler,
    private val getQueueItemDataUseCase: GetMojiPracticeQueueItemDataUseCase,
    analyticsManager: AnalyticsManager
) : BaseMojiPracticeQueue(
    practiceScope = coroutineScope,
    timeUtils = timeUtils,
    srsScheduler = srsScheduler,
    srsCardRepository = srsCardRepository,
    reviewHistoryRepository = reviewHistoryRepository,
    analyticsManager = analyticsManager,
    enableReviewUndo = true
), MojiPracticeQueue {

    override suspend fun undoLastReview(): Boolean = undoAnswer()

    override suspend fun MojiPracticeQueueItemDescriptor.toQueueItem(): MojiPracticeQueueItem {
        val srsCardKey = practiceType.toSrsKey(character)
        return MojiPracticeQueueItem(
            descriptor = this,
            srsCardKey = srsCardKey,
            srsCard = srsCardRepository.get(srsCardKey) ?: srsScheduler.newCard(),
            deckId = deckId,
            repeats = 0,
            totalMistakes = 0,
            data = coroutineScope.async(
                context = Dispatchers.IO,
                start = CoroutineStart.LAZY
            ) {
                getQueueItemDataUseCase(this@toQueueItem, coroutineScope)
            }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun createSummaryItem(
        queueItem: MojiPracticeQueueItem,
        totalReviews: Deferred<Int>
    ): MojiPracticeSummaryItem {
        return when (val data = queueItem.data.getCompleted()) {
            is MojiPracticeItemData.WritingData -> {
                MojiPracticeSummaryItem.Writing(
                    moji = queueItem.descriptor.character,
                    nextInterval = queueItem.srsCard.interval,
                    totalReviews = totalReviews,
                    strokeCount = data.strokes.size,
                    mistakes = queueItem.totalMistakes,
                )
            }

            is MojiPracticeItemData.ReadingData -> {
                MojiPracticeSummaryItem.Reading(
                    moji = queueItem.descriptor.character,
                    totalReviews = totalReviews,
                    nextInterval = queueItem.srsCard.interval
                )
            }

            else -> error("Unsupported")
        }
    }

    override fun getLoadingState(): MojiPracticeQueueState {
        return MojiPracticeQueueState.Loading
    }

    override suspend fun getReviewState(
        item: MojiPracticeQueueItem,
        answers: PracticeAnswers
    ): MojiPracticeQueueState {
        return MojiPracticeQueueState.Review(
            descriptor = item.descriptor,
            data = item.data.await(),
            currentItemRepeat = item.repeats,
            progress = getProgress(),
            answers = answers,
            canUndo = canUndo,
            undoCount = undoCount
        )
    }

    override fun getSummaryState(): MojiPracticeQueueState {
        val instant = timeUtils.now()
        return MojiPracticeQueueState.Summary(
            duration = instant - practiceStartInstant,
            items = summaryItems.values.toList(),
            canUndo = canUndo
        )
    }

}
