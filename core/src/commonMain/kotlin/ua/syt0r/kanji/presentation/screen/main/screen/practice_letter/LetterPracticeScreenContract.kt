package ua.syt0r.kanji.presentation.screen.main.screen.practice_letter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeQueueProgress
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeReviewState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeSummaryItem
import kotlin.time.Duration

interface LetterPracticeScreenContract {

    companion object {
        const val EXAMPLES_LOAD_PAGE_SIZE = 40
        const val EXAMPLES_PRELOAD_DISTANCE = 20
    }

    interface Content {

        @Composable
        operator fun invoke(
            configuration: LetterPracticeScreenConfiguration,
            mainNavigationState: MainNavigationState,
            viewModel: ViewModel
        )

    }

    interface ViewModel {

        val state: State<ScreenState>
        val reviewOperationInProgress: State<Boolean>
        val reviewErrors: Flow<ReviewError>

        fun initialize(configuration: LetterPracticeScreenConfiguration)
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
            val configuration: LetterPracticeConfiguration
        ) : ScreenState

        data class Review(
            val practiceProgress: PracticeQueueProgress,
            val reviewState: LetterPracticeReviewState,
            val canUndo: Boolean = false,
            val undoCount: Int = 0
        ) : ScreenState

        data class Summary(
            val duration: Duration,
            val accuracy: Float?,
            val items: List<LetterPracticeSummaryItem>,
            val canUndo: Boolean = false
        ) : ScreenState

    }

}
