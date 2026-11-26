package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeQueueProgress
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeReviewState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeSummaryItem
import kotlin.time.Duration

interface MojiPracticeScreenContract {

    companion object {
        const val EXAMPLES_LOAD_PAGE_SIZE = 40
        const val EXAMPLES_PRELOAD_DISTANCE = 20
    }

    interface Content {

        @Composable
        operator fun invoke(
            configuration: MojiPracticeScreenConfiguration,
            mainNavigationState: MainNavigationState,
            viewModel: ViewModel
        )

    }

    interface ViewModel {

        val state: State<ScreenState>
        val reviewOperationInProgress: State<Boolean>
        val reviewErrors: Flow<ReviewError>

        fun initialize(configuration: MojiPracticeScreenConfiguration)
        fun configure()

        fun submitAnswer(answer: PracticeAnswer)
        fun undoLastReview()
        fun speakKana(reading: KanaReading)
        fun finishPractice()

    }

    enum class ReviewError { SaveFailed, UndoFailed }

    sealed interface ScreenState {

        object Loading : ScreenState

        data class Configuring(
            val configuration: MojiPracticeConfiguration
        ) : ScreenState

        data class Review(
            val practiceProgress: PracticeQueueProgress,
            val reviewState: MojiPracticeReviewState,
            val canUndo: Boolean = false,
            val undoCount: Int = 0
        ) : ScreenState

        data class Summary(
            val duration: Duration,
            val accuracy: Float?,
            val items: List<MojiPracticeSummaryItem>,
            val canUndo: Boolean = false
        ) : ScreenState

    }

}
