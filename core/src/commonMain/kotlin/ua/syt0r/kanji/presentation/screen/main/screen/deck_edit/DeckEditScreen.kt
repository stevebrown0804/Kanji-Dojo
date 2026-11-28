package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ua.syt0r.kanji.presentation.dialog.MojiDeckConflictDialog
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.SuggestedVocabCardData
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardScreenMode

@Composable
fun DeckEditScreen(
    configuration: DeckEditScreenConfiguration,
    mainNavigationState: MainNavigationState,
    viewModel: DeckEditScreenContract.ViewModel = getMultiplatformViewModel()
) {

    val checkMembership = configuration is DeckEditScreenConfiguration.MojiDeck.Edit
    var membershipChecked by remember(viewModel) { mutableStateOf(!checkMembership) }

    LaunchedEffect(viewModel) {
        viewModel.initialize(configuration)

        val result = VocabCardScreenContract.VocabCardResultStorage.consumeResult()
        if (result != null) viewModel.notifyVocabCardResult(result)

        if (checkMembership) {
            viewModel.observeMojiDeckMembership { membershipChecked = true }
        }
    }

    DeckEditScreenUI(
        configuration = configuration,
        state = viewModel.state.collectAsState(),
        mojiMembershipChecked = membershipChecked,
        navigateBack = { mainNavigationState.navigateBack() },
        submitAdd = { viewModel.addCharacters(it) },
        dismissAddResult = { viewModel.dismissAddResult() },
        onItemClick = {

            val screenData = when (it) {
                is MojiDeckEditListItem -> InfoScreenData.Moji(it.character)
                is VocabDeckEditListItem -> {
                    val cardData = it.resultCardData.value
                    InfoScreenData.Vocab(
                        id = cardData.dictionaryId,
                        kanjiReading = cardData.kanjiReading,
                        kanaReading = cardData.kanaReading
                    )
                }
            }
            mainNavigationState.navigate(MainDestination.Info(screenData))
        },
        toggleRemoval = { viewModel.toggleRemoval(it) },
        editItem = {
            val destination = MainDestination.VocabCard(
                screenMode = VocabCardScreenMode.Edit(it.index),
                cardData = SuggestedVocabCardData(
                    cardId = it.savedVocabCard?.cardId,
                    cardData = it.resultCardData.value,
                    useDictionaryMeaningByDefault = true
                )
            )
            mainNavigationState.navigate(destination)
        },
        saveChanges = { viewModel.saveDeck() },
        deleteDeck = { viewModel.deleteDeck() },
        addNewVocabCardClick = {
            val destination = MainDestination.VocabCard(
                screenMode = VocabCardScreenMode.New,
                cardData = SuggestedVocabCardData()
            )
            mainNavigationState.navigate(destination)
        },
        onCompleted = {
            when {
                configuration is DeckEditScreenConfiguration.EditExisting && !it.wasDeleted -> {
                    mainNavigationState.navigateBack()
                }

                else -> mainNavigationState.popUpToHome()
            }
        },
    )

    MojiDeckConflictDialog(viewModel.mojiDeckConflictPrompt)

}
