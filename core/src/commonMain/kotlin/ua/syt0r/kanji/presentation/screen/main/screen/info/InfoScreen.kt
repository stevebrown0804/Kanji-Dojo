package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.Composable
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState


@Composable
fun InfoScreen(
    screenData: InfoScreenData,
    mainNavigationState: MainNavigationState,
    viewModel: InfoScreenContract.ViewModel = getMultiplatformViewModel(screenData)
) {

    InfoScreenUI(
        state = viewModel.state,
        onUpButtonClick = { mainNavigationState.navigateBack() },
        onMojiClick = {
            val nextScreenData = InfoScreenData.Moji(it)
            if (screenData != nextScreenData)
                mainNavigationState.navigate(MainDestination.Info(nextScreenData))
        },
        onWordClick = {
            val nextScreenData = it.toInfoScreenData()
            if (screenData != nextScreenData)
                mainNavigationState.navigate(MainDestination.Info(nextScreenData))
        },
        deckRemovalPrompt = viewModel.deckRemovalPrompt.value,
        onRequestDeckRemoval = viewModel::requestDeckRemoval,
        onConfirmDeckRemoval = viewModel::confirmDeckRemoval,
        onCancelDeckRemoval = viewModel::cancelDeckRemoval,
        deckTransfer = viewModel.deckTransfer.value,
        onRequestDeckTransfer = viewModel::requestDeckTransfer,
        onSelectDeckTransferMode = viewModel::selectDeckTransferMode,
        onConfirmDeckTransfer = viewModel::confirmDeckTransfer,
        onDismissDeckTransfer = viewModel::dismissDeckTransfer
    )

}
