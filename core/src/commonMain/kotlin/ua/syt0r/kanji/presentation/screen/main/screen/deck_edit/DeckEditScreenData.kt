package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.core.RefreshableData
import ua.syt0r.kanji.core.app_data.WordClassification
import ua.syt0r.kanji.core.japanese.CharacterClassification
import ua.syt0r.kanji.core.user_data.database.SavedVocabCard
import ua.syt0r.kanji.core.user_data.database.VocabCardData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.AddCharactersResult
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardEditResult

@Serializable
sealed interface DeckEditScreenConfiguration {

    interface EditExisting {
        val title: String
    }

    @Serializable
    @SerialName("ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration.LetterDeck")
    sealed interface MojiDeck : DeckEditScreenConfiguration {

        @Serializable
        @SerialName("ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration.LetterDeck.CreateNew")
        data object CreateNew : MojiDeck

        @Serializable
        @SerialName("ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration.LetterDeck.CreateDerived")
        data class CreateDerived(
            val title: String,
            val classification: CharacterClassification
        ) : MojiDeck

        @Serializable
        @SerialName("ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration.LetterDeck.Edit")
        data class Edit(
            override val title: String,
            @SerialName("letterDeckId")
            val mojiDeckId: Long
        ) : MojiDeck, EditExisting

    }

    @Serializable
    sealed interface VocabDeck : DeckEditScreenConfiguration {

        @Serializable
        data object CreateNew : VocabDeck

        @Serializable
        data class CreateDerived(
            val title: String,
            val classification: WordClassification
        ) : VocabDeck

        @Serializable
        data class Edit(
            override val title: String,
            val vocabDeckId: Long
        ) : VocabDeck, EditExisting

    }

}

sealed interface DeckEditListItem {
    val initialAction: DeckEditItemAction
    val action: MutableState<DeckEditItemAction>
}

data class MojiDeckEditListItem(
    val character: String,
    override val initialAction: DeckEditItemAction,
    override val action: MutableState<DeckEditItemAction>,
    val readings: List<String> = emptyList()
) : DeckEditListItem

data class VocabDeckEditListItem(
    val index: Int,
    val cardData: VocabCardData,
    val savedVocabCard: SavedVocabCard?,
    override val initialAction: DeckEditItemAction
) : DeckEditListItem {

    override val action: MutableState<DeckEditItemAction> = mutableStateOf(initialAction)
    val editResult: MutableState<VocabCardEditResult?> = mutableStateOf(null)

    val resultCardData: State<VocabCardData> = derivedStateOf {
        editResult.value?.cardData ?: cardData
    }

    val meaning = MutableStateFlow<RefreshableData<String>>(RefreshableData.Loading())

}

enum class DeckEditItemAction { Nothing, Add, Remove }

data class MutableMojiDeckEditingState(
    override val title: MutableState<String>,
    override val confirmExit: MutableState<Boolean>,
    override val adding: MutableState<Boolean>,
    override val listState: MutableState<List<MojiDeckEditListItem>>,
    override val lastAddResult: MutableState<AddCharactersResult?>
) : ScreenState.MojiDeckEditing

data class MutableVocabDeckEditingState(
    override val title: MutableState<String>,
    override val confirmExit: MutableState<Boolean>,
    override val list: MutableState<List<VocabDeckEditListItem>>
) : ScreenState.VocabDeckEditing
