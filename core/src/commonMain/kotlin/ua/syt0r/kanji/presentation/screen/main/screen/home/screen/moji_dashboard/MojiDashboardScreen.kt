package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard

import androidx.compose.runtime.Composable
import ua.syt0r.kanji.presentation.dialog.MojiDeckConflictDialog
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.data.DeckPickerScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeScreenConfiguration

@Composable
fun MojiDashboardScreen(
    mainNavigationState: MainNavigationState,
    viewModel: MojiDashboardScreenContract.ViewModel = getMultiplatformViewModel()
) {

    MojiDashboardScreenUI(
        state = viewModel.state,
        mergeDecks = { viewModel.mergeDecks(it) },
        sortDecks = { viewModel.sortDecks(it) },
        navigateToDeckDetails = {
            val configuration = DeckDetailsScreenConfiguration.MojiDeck(it.deckId)
            mainNavigationState.navigate(MainDestination.DeckDetails(configuration))
        },
        navigateToDeckEdit = {
            val configuration = DeckEditScreenConfiguration.MojiDeck.Edit(it.title, it.deckId)
            mainNavigationState.navigate(MainDestination.DeckEdit(configuration))
        },
        startQuickPractice = { item, practiceType, moji ->
            val destination: MainDestination.MojiPractice = MainDestination.MojiPractice(
                configuration = MojiPracticeScreenConfiguration(
                    cards = moji.map { MojiPracticeScreenConfiguration.Card(it, item.deckId) },
                    practiceType = practiceType
                )
            )
            mainNavigationState.navigate(destination)
        },
        navigateToDeckPicker = {
            val destination = MainDestination.DeckPicker(DeckPickerScreenConfiguration.Moji)
            mainNavigationState.navigate(destination)
        }
    )

    MojiDeckConflictDialog(viewModel.mojiDeckConflictPrompt)

}
