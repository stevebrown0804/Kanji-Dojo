package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.user_data.database.LetterDeckConflictResolution
import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.presentation.dialog.LetterDeckConflictPrompt
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.AddCharactersResult
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DeleteDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.LoadDeckEditLetterDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.LoadDeckEditVocabDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.SaveDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.ValidateDeckCharactersUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardEditResult

class DeckEditViewModel(
    private val viewModelScope: CoroutineScope,
    private val loadDeckEditLetterDataUseCase: LoadDeckEditLetterDataUseCase,
    private val loadDeckEditVocabDataUseCase: LoadDeckEditVocabDataUseCase,
    private val validateDeckCharactersUseCase: ValidateDeckCharactersUseCase,
    private val letterPracticeRepository: LetterPracticeRepository,
    private val saveDeckUseCase: SaveDeckUseCase,
    private val deleteDeckUseCase: DeleteDeckUseCase,
    private val analyticsManager: AnalyticsManager
) : DeckEditScreenContract.ViewModel {

    private lateinit var configuration: DeckEditScreenConfiguration

    private val deckTitle = mutableStateOf("")
    private val wasDeckEdited = mutableStateOf(false)
    private val approvedMoves = mutableMapOf<String, LetterDeckConflictResolution.Move>()
    override val letterDeckConflictPrompt = LetterDeckConflictPrompt()

    private lateinit var letterEditingState: MutableLetterDeckEditingState
    private lateinit var vocabDeckEditingState: MutableVocabDeckEditingState

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    override fun initialize(configuration: DeckEditScreenConfiguration) {
        if (this::configuration.isInitialized) return

        this.configuration = configuration
        viewModelScope.launch {

            val defaultListItemAction = when (configuration) {
                is DeckEditScreenConfiguration.EditExisting -> DeckEditItemAction.Nothing
                else -> DeckEditItemAction.Add
            }

            when (configuration) {
                is DeckEditScreenConfiguration.LetterDeck -> {
                    val letterData = loadDeckEditLetterDataUseCase(configuration)
                    val addResult = validateDeckCharactersUseCase(letterData.characters)

                    deckTitle.value = letterData.title ?: ""

                    letterEditingState = MutableLetterDeckEditingState(
                        title = deckTitle,
                        confirmExit = wasDeckEdited,
                        adding = mutableStateOf(false),
                        listState = mutableStateOf(emptyList()),
                        lastAddResult = mutableStateOf(addResult),
                    )

                    addLettersToEditList(
                        addResult = addResult,
                        defaultAction = defaultListItemAction
                    )

                    _state.value = letterEditingState

                    reportInvalidImportCharacter(addResult.unknownCharacters)
                }

                is DeckEditScreenConfiguration.VocabDeck -> {
                    val vocabData = loadDeckEditVocabDataUseCase(
                        configuration = configuration,
                        defaultListItemAction = defaultListItemAction,
                        viewModelScope = viewModelScope
                    )
                    deckTitle.value = vocabData.title ?: ""

                    vocabDeckEditingState = MutableVocabDeckEditingState(
                        title = deckTitle,
                        confirmExit = wasDeckEdited,
                        list = mutableStateOf(vocabData.items)
                    )
                    _state.value = vocabDeckEditingState
                }
            }

        }
    }

    override fun notifyVocabCardResult(
        editResult: VocabCardEditResult
    ) {
        wasDeckEdited.value = true
        when (editResult) {
            is VocabCardEditResult.Existing -> {
                vocabDeckEditingState.list.value[editResult.index].editResult.value = editResult
            }

            is VocabCardEditResult.New -> viewModelScope.launch {
                val list = vocabDeckEditingState.list.value
                val newItem = VocabDeckEditListItem(
                    index = list.size,
                    cardData = editResult.cardData,
                    savedVocabCard = null,
                    initialAction = DeckEditItemAction.Add
                ).also { loadDeckEditVocabDataUseCase.handleMeaningLoading(it, viewModelScope) }
                vocabDeckEditingState.list.value = list.plus(newItem)
            }
        }

    }

    override fun addCharacters(input: String) {
        val editingState = _state.value as? MutableLetterDeckEditingState ?: return
        if (editingState.adding.value) return

        editingState.adding.value = true
        viewModelScope.launch {
            try {
                val addResult = validateDeckCharactersUseCase(input)
                val currentCharacters = editingState.getCurrentList().map { it.character }.toSet()
                val newCharacters = addResult.detectedCharacter
                    .filterNot { it in currentCharacters }
                    .toMutableSet()

                if (newCharacters.isNotEmpty()) {
                    val targetDeckId = (configuration as? DeckEditScreenConfiguration.LetterDeck.Edit)
                        ?.letterDeckId
                    val conflicts = letterPracticeRepository.getDeckConflicts(
                        newCharacters.toList(),
                        targetDeckId
                    )
                    for (conflict in conflicts) {
                        when (val decision = letterDeckConflictPrompt.choose(conflict)) {
                            LetterDeckConflictResolution.Leave -> newCharacters.remove(conflict.character)
                            is LetterDeckConflictResolution.Move -> approvedMoves[conflict.character] = decision
                        }
                    }
                }

                if (newCharacters.isNotEmpty()) {
                    addLettersToEditList(addResult, DeckEditItemAction.Add, newCharacters)
                    wasDeckEdited.value = true
                }
                editingState.lastAddResult.value = addResult
            } finally {
                editingState.adding.value = false
            }
        }
    }

    override fun dismissAddResult() {
        letterEditingState.lastAddResult.value = null
    }

    private fun addLettersToEditList(
        addResult: AddCharactersResult,
        defaultAction: DeckEditItemAction,
        charactersToAdd: Iterable<String> = addResult.detectedCharacter
    ) {
        val newListItems = charactersToAdd.map { character ->
            LetterDeckEditListItem(
                character = character,
                initialAction = defaultAction,
                action = mutableStateOf(defaultAction),
                readings = addResult.readingsByCharacter[character].orEmpty()
            )
        }
        letterEditingState.listState.value = letterEditingState.listState.value.plus(newListItems)
    }

    override fun toggleRemoval(item: DeckEditListItem) {
        item.action.value = when (item.action.value) {
            DeckEditItemAction.Remove -> item.initialAction
            DeckEditItemAction.Add,
            DeckEditItemAction.Nothing -> DeckEditItemAction.Remove
        }
        wasDeckEdited.value = true
    }

    override fun saveDeck() {
        val loadedState = _state.value as? ScreenState.Loaded ?: return
        if (loadedState is ScreenState.LetterDeckEditing && loadedState.adding.value) return

        val title = deckTitle.value
        val list = loadedState.getCurrentList()
        _state.value = ScreenState.SavingChanges
        viewModelScope.launch {
            letterDeckConflictPrompt.save(approvedMoves) { resolutions ->
                saveDeckUseCase(configuration, title, list, resolutions)
            }
            _state.value = ScreenState.Completed(false)
        }
    }

    override fun deleteDeck() {
        _state.value = ScreenState.Deleting
        viewModelScope.launch {
            deleteDeckUseCase(configuration)
            _state.value = ScreenState.Completed(true)
        }
    }

    private fun reportInvalidImportCharacter(characters: List<String>) {
        characters.forEach {
            analyticsManager.sendEvent("import_unknown_character") {
                put("character", it)
            }
        }
    }

}
