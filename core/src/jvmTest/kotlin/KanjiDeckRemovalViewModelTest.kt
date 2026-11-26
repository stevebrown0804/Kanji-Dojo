import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withTimeout
import ua.syt0r.kanji.core.analytics.PrintAnalyticsManager
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightMojiPracticeRepository
import ua.syt0r.kanji.presentation.common.Paginateable
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiRadicalsSectionData
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.info.MojiInfoData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KanjiDeckRemovalViewModelTest {

    @Test
    fun cancelAndAbandonPreserveDeckMembership() = withRemovalViewModel(1) { viewModel, repository, job ->
        val deck = repository.getDecks().single()
        viewModel.requestDeckRemoval(deck)
        viewModel.cancelDeckRemoval(assertNotNull(viewModel.deckRemovalPrompt.value))
        job.awaitPendingWork()
        assertNull(viewModel.deckRemovalPrompt.value)
        assertEquals(listOf("休"), repository.getDeckCharacters(deck.id))

        viewModel.requestDeckRemoval(deck)
        viewModel.confirmDeckRemoval(assertNotNull(viewModel.deckRemovalPrompt.value))
        job.awaitPendingWork()
        val lastDeckPrompt = assertNotNull(viewModel.deckRemovalPrompt.value)
        assertTrue(lastDeckPrompt.isLastDeckConfirmation)
        viewModel.cancelDeckRemoval(lastDeckPrompt)
        job.awaitPendingWork()
        assertNull(viewModel.deckRemovalPrompt.value)
        assertEquals(listOf("休"), repository.getDeckCharacters(deck.id))
    }

    @Test
    fun proceedRemovesOnlyTheSelectedMembershipWhenOtherDecksRemain() =
        withRemovalViewModel(2) { viewModel, repository, job ->
            val decks = repository.getDecks()
            viewModel.requestDeckRemoval(decks.first())
            val prompt = assertNotNull(viewModel.deckRemovalPrompt.value)
            assertFalse(prompt.isLastDeckConfirmation)
            viewModel.confirmDeckRemoval(prompt)
            job.awaitPendingWork()

            assertNull(viewModel.deckRemovalPrompt.value)
            assertEquals(emptyList(), repository.getDeckCharacters(decks.first().id))
            assertEquals(listOf("休"), repository.getDeckCharacters(decks.last().id))
        }

    @Test
    fun lastMembershipRequiresTheSecondConfirmationAndIgnoresStaleEvents() =
        withRemovalViewModel(1) { viewModel, repository, job ->
            val deck = repository.getDecks().single()
            viewModel.requestDeckRemoval(deck)
            val firstPrompt = assertNotNull(viewModel.deckRemovalPrompt.value)
            assertFalse(firstPrompt.isLastDeckConfirmation)

            viewModel.confirmDeckRemoval(firstPrompt)
            job.awaitPendingWork()
            val lastDeckPrompt = assertNotNull(viewModel.deckRemovalPrompt.value)
            assertTrue(lastDeckPrompt.isLastDeckConfirmation)
            assertEquals(listOf("休"), repository.getDeckCharacters(deck.id))

            viewModel.confirmDeckRemoval(firstPrompt)
            viewModel.cancelDeckRemoval(firstPrompt)
            job.awaitPendingWork()
            assertSame(lastDeckPrompt, viewModel.deckRemovalPrompt.value)
            assertEquals(listOf("休"), repository.getDeckCharacters(deck.id))

            viewModel.confirmDeckRemoval(lastDeckPrompt)
            job.awaitPendingWork()
            assertNull(viewModel.deckRemovalPrompt.value)
            assertEquals(emptyList(), repository.getDeckCharacters(deck.id))
        }

    private fun withRemovalViewModel(
        deckCount: Int,
        block: suspend (InfoScreenViewModel, SqlDelightMojiPracticeRepository, Job) -> Unit
    ) = withDatabase { manager, _ ->
        val repository = SqlDelightMojiPracticeRepository(manager)
        repeat(deckCount) { index -> repository.createDeck("Deck ${index + 1}", listOf("休")) }
        val job = SupervisorJob()
        val data = kanjiData(repository.getDecks())
        try {
            val viewModel = InfoScreenViewModel(
                viewModelScope = CoroutineScope(job + Dispatchers.Unconfined),
                screenData = InfoScreenData.Moji("休"),
                loadMojiStateUseCase = object : InfoScreenContract.LoadMojiStateUseCase {
                    override suspend fun invoke(character: String, coroutineScope: CoroutineScope) =
                        ScreenState.Loaded.Moji(data)
                },
                loadVocabStateUseCase = object : InfoScreenContract.LoadVocabStateUseCase {
                    override suspend fun invoke(data: InfoScreenData.Vocab, coroutineScope: CoroutineScope): ScreenState =
                        error("This test only opens Kanji Info")
                },
                analyticsManager = PrintAnalyticsManager(),
                mojiPracticeRepository = repository
            )
            job.awaitPendingWork()
            block(viewModel, repository, job)
        } finally {
            job.cancelAndJoin()
        }
    }

    private fun kanjiData(decks: List<MojiDeck>) = MojiInfoData.Kanji(
        character = "休",
        strokes = emptyList(),
        on = emptyList(),
        kun = emptyList(),
        meanings = emptyList(),
        grade = null,
        jlptLevel = null,
        frequency = null,
        radicalsSectionData = KanjiRadicalsSectionData(emptyList(), emptyList()),
        displayRadicals = emptyList(),
        containingDecks = MutableStateFlow(decks),
        vocab = EmptyRemovalPagination(),
        sentences = EmptyRemovalPagination()
    )

    private suspend fun Job.awaitPendingWork() {
        withTimeout(5_000) { children.toList().joinAll() }
    }
}

private class EmptyRemovalPagination<T> : Paginateable<T> {
    override val total = 0
    override val list = MutableStateFlow(emptyList<T>())
    override val canLoadMore = MutableStateFlow(false)
    override fun loadMore() = Unit
}
