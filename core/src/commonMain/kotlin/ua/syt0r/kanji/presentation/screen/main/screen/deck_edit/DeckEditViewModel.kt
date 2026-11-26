package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.user_data.database.MojiDeckConflictResolution
import ua.syt0r.kanji.core.user_data.database.MojiPracticeRepository
import ua.syt0r.kanji.presentation.dialog.MojiDeckConflictPrompt
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.AddCharactersResult
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DeleteDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.LoadDeckEditMojiDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.LoadDeckEditVocabDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.SaveDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.ValidateDeckCharactersUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardEditResult

class DeckEditViewModel(
    private val viewModelScope: CoroutineScope,
    private val loadDeckEditMojiDataUseCase: LoadDeckEditMojiDataUseCase,
    private val loadDeckEditVocabDataUseCase: LoadDeckEditVocabDataUseCase,
    private val validateDeckCharactersUseCase: ValidateDeckCharactersUseCase,
    private val mojiPracticeRepository: MojiPracticeRepository,
    private val saveDeckUseCase: SaveDeckUseCase,
    private val deleteDeckUseCase: DeleteDeckUseCase,
    private val analyticsManager: AnalyticsManager
) : DeckEditScreenContract.ViewModel {

    private lateinit var configuration: DeckEditScreenConfiguration

    private val deckTitle = mutableStateOf("")
    private val wasDeckEdited = mutableStateOf(false)
    private val approvedMoves = mutableMapOf<String, MojiDeckConflictResolution.Move>()
    override val mojiDeckConflictPrompt = MojiDeckConflictPrompt()

    private lateinit var mojiEditingState: MutableMojiDeckEditingState
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
                is DeckEditScreenConfiguration.MojiDeck -> {
                    val mojiData = loadDeckEditMojiDataUseCase(configuration)
                    val addResult = validateDeckCharactersUseCase(mojiData.characters)

                    deckTitle.value = mojiData.title ?: ""

                    mojiEditingState = MutableMojiDeckEditingState(
                        title = deckTitle,
                        confirmExit = wasDeckEdited,
                        adding = mutableStateOf(false),
                        listState = mutableStateOf(emptyList()),
                        lastAddResult = mutableStateOf(addResult),
                    )

                    addMojiToEditList(
                        addResult = addResult,
                        defaultAction = defaultListItemAction
                    )

                    _state.value = mojiEditingState

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
        val editingState = _state.value as? MutableMojiDeckEditingState ?: return
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
                    val targetDeckId = (configuration as? DeckEditScreenConfiguration.MojiDeck.Edit)
                        ?.mojiDeckId
                    val conflicts = mojiPracticeRepository.getDeckConflicts(
                        newCharacters.toList(),
                        targetDeckId
                    )
                    for (conflict in conflicts) {
                        when (val decision = mojiDeckConflictPrompt.choose(conflict)) {
                            MojiDeckConflictResolution.Leave -> newCharacters.remove(conflict.character)
                            is MojiDeckConflictResolution.Move -> approvedMoves[conflict.character] = decision
                        }
                    }
                }

                if (newCharacters.isNotEmpty()) {
                    addMojiToEditList(addResult, DeckEditItemAction.Add, newCharacters)
                    wasDeckEdited.value = true
                }
                editingState.lastAddResult.value = addResult
            } finally {
                editingState.adding.value = false
            }
        }
    }

    override fun dismissAddResult() {
        mojiEditingState.lastAddResult.value = null
    }

    private fun addMojiToEditList(
        addResult: AddCharactersResult,
        defaultAction: DeckEditItemAction,
        charactersToAdd: Iterable<String> = addResult.detectedCharacter
    ) {
        val newListItems = charactersToAdd.map { character ->
            MojiDeckEditListItem(
                character = character,
                initialAction = defaultAction,
                action = mutableStateOf(defaultAction),
                readings = addResult.readingsByCharacter[character].orEmpty()
            )
        }
        mojiEditingState.listState.value = mojiEditingState.listState.value.plus(newListItems)
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
        if (loadedState is ScreenState.MojiDeckEditing && loadedState.adding.value) return

        val title = deckTitle.value
        val list = loadedState.getCurrentList()
        _state.value = ScreenState.SavingChanges
        viewModelScope.launch {
            mojiDeckConflictPrompt.save(approvedMoves) { resolutions ->
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
