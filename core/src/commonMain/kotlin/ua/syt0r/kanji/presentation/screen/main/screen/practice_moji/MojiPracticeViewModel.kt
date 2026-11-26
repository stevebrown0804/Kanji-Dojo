package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.tts.KanaTtsManager
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.MojiPracticeScreenContract.ReviewError
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.MojiPracticeScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeItemData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeReviewState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeSummaryItem
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeConfigurationUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeQueueDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeReviewStateUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.UpdateMojiPracticeConfigurationUseCase


class MojiPracticeViewModel(
    private val viewModelScope: CoroutineScope,
    private val getConfigurationUseCase: GetMojiPracticeConfigurationUseCase,
    private val updateConfigurationUseCase: UpdateMojiPracticeConfigurationUseCase,
    private val getQueueDataUseCase: GetMojiPracticeQueueDataUseCase,
    private val practiceQueue: MojiPracticeQueue,
    private val getReviewStateUseCase: GetMojiPracticeReviewStateUseCase,
    private val analyticsManager: AnalyticsManager,
    private val kanaTtsManager: KanaTtsManager
) : MojiPracticeScreenContract.ViewModel {

    private lateinit var configuration: MojiPracticeScreenConfiguration

    private val _state = mutableStateOf<ScreenState>(ScreenState.Loading)
    override val state: State<ScreenState> = _state
    private val _reviewOperationInProgress = mutableStateOf(false)
    override val reviewOperationInProgress: State<Boolean> = _reviewOperationInProgress
    private val reviewErrorChannel = Channel<ReviewError>(Channel.BUFFERED)
    override val reviewErrors: Flow<ReviewError> = reviewErrorChannel.receiveAsFlow()
    private var kanaAutoReadJob: Job? = null
    private var displayedQueueState: MojiPracticeQueueState? = null

    override fun initialize(configuration: MojiPracticeScreenConfiguration) {
        if (this::configuration.isInitialized) return
        this.configuration = configuration

        viewModelScope.launch {
            _state.value = ScreenState.Configuring(
                configuration = getConfigurationUseCase(configuration)
            )
        }
    }

    override fun configure() {
        val configurationState = _state.value as ScreenState.Configuring
        _state.value = ScreenState.Loading

        viewModelScope.launch {

            updateConfigurationUseCase(configurationState.configuration)

            practiceQueue.initialize(
                items = getQueueDataUseCase(configurationState.configuration)
            )

            practiceQueue.state
                .onEach { showQueueState(it) }
                .launchIn(this)
        }

        reportConfiguration(configurationState)
    }

    override fun submitAnswer(answer: PracticeAnswer) {
        if (_state.value !is ScreenState.Review) return
        runReviewOperation(ReviewError.SaveFailed) {
            practiceQueue.submitAnswer(answer)
            true
        }
    }

    override fun undoLastReview() {
        val canUndo = when (val currentState = _state.value) {
            is ScreenState.Review -> currentState.canUndo
            is ScreenState.Summary -> currentState.canUndo
            else -> false
        }
        if (!canUndo) return
        runReviewOperation(ReviewError.UndoFailed) { practiceQueue.undoLastReview() }
    }

    private fun runReviewOperation(error: ReviewError, operation: suspend () -> Boolean) {
        if (_reviewOperationInProgress.value) return
        _reviewOperationInProgress.value = true
        viewModelScope.launch {
            try {
                if (!operation()) reviewErrorChannel.trySend(error)
                showQueueState(practiceQueue.state.value)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Logger.e(exception.stackTraceToString())
                reviewErrorChannel.trySend(error)
            } finally {
                _reviewOperationInProgress.value = false
            }
        }
    }

    private fun showQueueState(queueState: MojiPracticeQueueState) {
        if (displayedQueueState === queueState) return
        displayedQueueState = queueState
        kanaAutoReadJob?.cancel()
        _state.value = when (queueState) {
            MojiPracticeQueueState.Loading -> ScreenState.Loading
            is MojiPracticeQueueState.Summary -> queueState.toScreenState()
            is MojiPracticeQueueState.Review -> queueState.toScreenState().also { reviewState ->
                kanaAutoReadJob = reviewState.kanaAutoReadFlow()
                    .onEach { speakKana(it) }
                    .launchIn(viewModelScope)
            }
        }
    }

    override fun speakKana(reading: KanaReading) {
        viewModelScope.launch { kanaTtsManager.speak(reading) }
    }

    override fun finishPractice() {
        if (_reviewOperationInProgress.value) return
        practiceQueue.immediateFinish()
        showQueueState(practiceQueue.state.value)
    }

    private fun reportConfiguration(state: ScreenState.Configuring) {
        analyticsManager.sendEvent("moji_practice_configuration") {
            put("practice_type", configuration.practiceType.dataType.srsPracticeType.value)
            put("list_size", state.configuration.selectorState.selectedCountIntState.value)
        }
    }

    private fun MojiPracticeQueueState.Review.toScreenState(): ScreenState.Review {
        return ScreenState.Review(
            practiceProgress = progress,
            reviewState = getReviewStateUseCase(this),
            canUndo = canUndo,
            undoCount = undoCount
        )
    }

    private fun MojiPracticeQueueState.Summary.toScreenState(): ScreenState.Summary {
        val accuracy: Float? = items.filterIsInstance<MojiPracticeSummaryItem.Writing>()
            .takeIf { it.isNotEmpty() }
            ?.let {
                val totalStrokeCount = it.fold(0) { sum, item -> sum + item.strokeCount }
                val totalMistakeCount = it.fold(0) { sum, item -> sum + item.mistakes }
                val correctStrokes = (totalStrokeCount - totalMistakeCount)
                    .coerceAtLeast(0)
                correctStrokes.toFloat() * 100 / totalStrokeCount
            }
        return ScreenState.Summary(
            duration = duration,
            accuracy = accuracy,
            items = items,
            canUndo = canUndo
        )
    }

    private fun ScreenState.Review.kanaAutoReadFlow(): Flow<KanaReading> = callbackFlow {
        when {
            reviewState is MojiPracticeReviewState.Reading &&
                    reviewState.itemData is MojiPracticeItemData.KanaReadingData -> {

                snapshotFlow { reviewState.revealed.value }
                    .filter { it && reviewState.layout.kanaAutoPlay.value }
                    .take(1)
                    .onEach { send(reviewState.itemData.reading) }
                    .collect()

            }

            reviewState is MojiPracticeReviewState.Writing &&
                    reviewState.itemData is MojiPracticeItemData.KanaWritingData -> {

                // Plays when writer state is switched (study/review)
                snapshotFlow { reviewState.writerState.value }
                    .filter { reviewState.layout.kanaAutoPlay.value }
                    .onEach {
                        delay(200)
                        send(reviewState.itemData.reading)
                    }
                    .collect()

            }
        }
        awaitClose()
    }

}
