import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ua.syt0r.kanji.core.analytics.PrintAnalyticsManager
import ua.syt0r.kanji.core.user_data.database.KanjiDeckTransferMode
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightMojiPracticeRepository
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightVocabPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditItemAction
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.VocabDeckEditListItem
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.AddCharactersResult
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DeckEditMojiData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DeckEditVocabData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DefaultDeleteDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DefaultSaveDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.LoadDeckEditMojiDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.LoadDeckEditVocabDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.ValidateDeckCharactersUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeckEditMembershipTest {

    @Test
    fun returningFromInfoUpdatesMembershipsAndPreservesTheEditDraft() = withEditor { repository, model, scope ->
        repository.createDeck("Editing", listOf("日", "月", "火"))
        repository.createDeck("Other", listOf("水"))
        val editingDeck = repository.deckId("Editing")
        val otherDeck = repository.deckId("Other")
        model.initialize(DeckEditScreenConfiguration.MojiDeck.Edit("Editing", editingDeck))
        val state = model.editingState()
        state.title.value = "Draft title"
        model.toggleRemoval(state.listState.value.single { it.character == "月" })
        model.addCharacters("木")
        assertFalse(state.adding.value)
        val retainedDraftOrder = state.listState.value.map { it.character }.filter { it != "日" }

        var observation = MembershipObservation(scope, model)
        observation.awaitUpdate()
        observation.stop()
        assertTrue(repository.transferKanjiBetweenDecks("日", editingDeck, otherDeck, KanjiDeckTransferMode.Move))
        assertTrue(repository.transferKanjiBetweenDecks("水", otherDeck, editingDeck, KanjiDeckTransferMode.Copy))

        observation = MembershipObservation(scope, model)
        observation.awaitUpdate()
        val items = state.listState.value.associateBy { it.character }
        assertEquals(setOf("月", "火", "木", "水"), items.keys)
        assertEquals(retainedDraftOrder, state.listState.value.map { it.character }.filter { it != "水" })
        assertEquals("Draft title", state.title.value)
        assertTrue(state.confirmExit.value)
        assertEquals(DeckEditItemAction.Remove, items.getValue("月").action.value)
        assertEquals(DeckEditItemAction.Add, items.getValue("木").initialAction)
        assertEquals(DeckEditItemAction.Add, items.getValue("木").action.value)
        assertEquals(DeckEditItemAction.Nothing, items.getValue("水").initialAction)
        assertEquals(DeckEditItemAction.Nothing, items.getValue("水").action.value)
        assertEquals(listOf("reading-水"), items.getValue("水").readings)

        model.saveDeck()
        withTimeout(5_000) { model.state.filterIsInstance<ScreenState.Completed>().first() }
        assertEquals(setOf("火", "木", "水"), repository.getDeckCharacters(editingDeck).toSet())
        assertEquals(setOf("日", "水"), repository.getDeckCharacters(otherDeck).toSet())
        assertEquals("Draft title", repository.getDeck(editingDeck).name)
        observation.stop()
    }

    @Test
    fun additionsCommittedInInfoBecomeBaselineAndDoNotReturnAfterLaterMoves() = withEditor { repository, model, scope ->
        repository.createDeck("Editing", listOf("日"))
        repository.createDeck("Other", listOf("月", "火"))
        val editingDeck = repository.deckId("Editing")
        val otherDeck = repository.deckId("Other")
        model.initialize(DeckEditScreenConfiguration.MojiDeck.Edit("Editing", editingDeck))
        val state = model.editingState()
        model.addCharacters("月火")
        assertFalse(state.adding.value)
        model.toggleRemoval(state.listState.value.single { it.character == "火" })

        var observation = MembershipObservation(scope, model)
        observation.awaitUpdate()
        observation.stop()
        assertTrue(repository.transferKanjiBetweenDecks("月", otherDeck, editingDeck, KanjiDeckTransferMode.Copy))
        assertTrue(repository.transferKanjiBetweenDecks("火", otherDeck, editingDeck, KanjiDeckTransferMode.Copy))

        observation = MembershipObservation(scope, model)
        observation.awaitUpdate()
        assertEquals(setOf("日", "月", "火"), state.listState.value.map { it.character }.toSet())
        state.listState.value.forEach {
            assertEquals(DeckEditItemAction.Nothing, it.initialAction)
            assertEquals(DeckEditItemAction.Nothing, it.action.value)
        }
        observation.stop()

        assertTrue(repository.transferKanjiBetweenDecks("月", editingDeck, otherDeck, KanjiDeckTransferMode.Move))
        assertTrue(repository.transferKanjiBetweenDecks("火", editingDeck, otherDeck, KanjiDeckTransferMode.Move))
        observation = MembershipObservation(scope, model)
        observation.awaitUpdate()
        assertEquals(listOf("日"), state.listState.value.map { it.character })

        model.saveDeck()
        withTimeout(5_000) { model.state.filterIsInstance<ScreenState.Completed>().first() }
        assertEquals(listOf("日"), repository.getDeckCharacters(editingDeck))
        assertEquals(setOf("月", "火"), repository.getDeckCharacters(otherDeck).toSet())
        observation.stop()
    }

    private fun withEditor(
        block: suspend (SqlDelightMojiPracticeRepository, DeckEditViewModel, CoroutineScope) -> Unit
    ) = withDatabase { manager, _ ->
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Unconfined)
        val mojiRepository = SqlDelightMojiPracticeRepository(manager)
        val vocabRepository = SqlDelightVocabPracticeRepository(manager)
        val model = DeckEditViewModel(
            viewModelScope = scope,
            loadDeckEditMojiDataUseCase = object : LoadDeckEditMojiDataUseCase {
                override suspend fun invoke(configuration: DeckEditScreenConfiguration.MojiDeck): DeckEditMojiData {
                    val edit = assertIs<DeckEditScreenConfiguration.MojiDeck.Edit>(configuration)
                    return DeckEditMojiData(
                        title = mojiRepository.getDeck(edit.mojiDeckId).name,
                        characters = mojiRepository.getDeckCharacters(edit.mojiDeckId)
                    )
                }
            },
            loadDeckEditVocabDataUseCase = UnusedVocabLoader,
            validateDeckCharactersUseCase = KnownCharactersValidator,
            mojiPracticeRepository = mojiRepository,
            saveDeckUseCase = DefaultSaveDeckUseCase(mojiRepository, vocabRepository),
            deleteDeckUseCase = DefaultDeleteDeckUseCase(mojiRepository, vocabRepository),
            analyticsManager = PrintAnalyticsManager()
        )
        try {
            block(mojiRepository, model, scope)
        } finally {
            job.cancelAndJoin()
        }
    }

    private suspend fun SqlDelightMojiPracticeRepository.deckId(name: String): Long =
        getDecks().single { it.name == name }.id

    private suspend fun DeckEditViewModel.editingState(): ScreenState.MojiDeckEditing =
        withTimeout(5_000) { state.filterIsInstance<ScreenState.MojiDeckEditing>().first() }

    private class MembershipObservation(scope: CoroutineScope, model: DeckEditViewModel) {
        private val updates = Channel<Unit>(Channel.UNLIMITED)
        private val job = scope.launch {
            model.observeMojiDeckMembership { updates.trySend(Unit).getOrThrow() }
        }

        suspend fun awaitUpdate() {
            withTimeout(5_000) { updates.receive() }
        }

        suspend fun stop() {
            job.cancelAndJoin()
            updates.close()
        }
    }

    private object KnownCharactersValidator : ValidateDeckCharactersUseCase {
        override suspend fun invoke(input: String): AddCharactersResult =
            invoke(input.map { it.toString() })

        override suspend fun invoke(character: List<String>): AddCharactersResult = AddCharactersResult(
            detectedCharacter = character.distinct(),
            unknownCharacters = emptyList(),
            readingsByCharacter = character.associateWith { listOf("reading-$it") }
        )
    }

    private object UnusedVocabLoader : LoadDeckEditVocabDataUseCase {
        override suspend fun invoke(
            configuration: DeckEditScreenConfiguration.VocabDeck,
            defaultListItemAction: DeckEditItemAction,
            viewModelScope: CoroutineScope
        ): DeckEditVocabData = error("Vocabulary loading is outside these moji editor tests.")

        override fun handleMeaningLoading(item: VocabDeckEditListItem, scope: CoroutineScope): Unit =
            error("Vocabulary loading is outside these moji editor tests.")
    }
}
