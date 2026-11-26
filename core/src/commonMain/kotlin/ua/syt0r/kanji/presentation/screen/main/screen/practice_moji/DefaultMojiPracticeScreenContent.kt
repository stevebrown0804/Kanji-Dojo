package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.info.toInfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.ui.MojiPracticeScreenUI

object DefaultMojiPracticeScreenContent : MojiPracticeScreenContract.Content {

    @Composable
    override fun invoke(
        configuration: MojiPracticeScreenConfiguration,
        mainNavigationState: MainNavigationState,
        viewModel: MojiPracticeScreenContract.ViewModel
    ) {

        LaunchedEffect(Unit) {
            viewModel.initialize(configuration)
        }

        MojiPracticeScreenUI(
            state = viewModel.state,
            reviewOperationInProgress = viewModel.reviewOperationInProgress,
            reviewErrors = viewModel.reviewErrors,
            navigateBack = { mainNavigationState.navigateBack() },
            onConfigured = { viewModel.configure() },
            speakKana = { viewModel.speakKana(it) },
            onNextClick = { viewModel.submitAnswer(it) },
            onUndoClick = { viewModel.undoLastReview() },
            onWordClick = {
                val destination = MainDestination.Info(it.toInfoScreenData())
                mainNavigationState.navigate(destination)
            },
            onSummaryItemCLick = {
                mainNavigationState.navigate(MainDestination.Info(InfoScreenData.Moji(it.moji)))
            },
            finishPractice = { viewModel.finishPractice() },
            onPracticeCompleted = { mainNavigationState.navigateBack() },
        )

    }

}
