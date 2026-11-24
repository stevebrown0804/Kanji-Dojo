package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.AddCharactersResult
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardEditResult

interface DeckEditScreenContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>
        fun initialize(configuration: DeckEditScreenConfiguration)
        fun notifyVocabCardResult(editResult: VocabCardEditResult)
        fun addCharacters(input: String)
        fun dismissAddResult()
        fun toggleRemoval(item: DeckEditListItem)
        fun saveDeck()
        fun deleteDeck()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        sealed interface Loaded : ScreenState {
            val title: MutableState<String>
            val confirmExit: State<Boolean>
            fun getCurrentList(): List<DeckEditListItem>
        }

        interface LetterDeckEditing : Loaded {
            val adding: State<Boolean>
            val listState: State<List<LetterDeckEditListItem>>
            val lastAddResult: State<AddCharactersResult?>
            override fun getCurrentList(): List<LetterDeckEditListItem> {
                return listState.value
            }
        }

        interface VocabDeckEditing : Loaded {
            val list: State<List<VocabDeckEditListItem>>
            override fun getCurrentList(): List<VocabDeckEditListItem> = list.value
        }

        data object SavingChanges : ScreenState
        data object Deleting : ScreenState

        data class Completed(val wasDeleted: Boolean) : ScreenState

    }

}

