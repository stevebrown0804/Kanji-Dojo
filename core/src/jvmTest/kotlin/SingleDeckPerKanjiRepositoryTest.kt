import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.SrsPracticeType
import ua.syt0r.kanji.core.user_data.database.MojiDeckConflictResolution
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightMojiPracticeRepository
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightReviewHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SingleDeckPerKanjiRepositoryTest {

    @Test
    fun switchOffAllowsDuplicatesAndSwitchOnChecksOnlyNewKanji() = withDatabase { manager, _ ->
        val repository = SqlDelightMojiPracticeRepository(manager)
        repository.createDeck("Source", listOf("休", "国", "ひ"))
        assertTrue(repository.getDeckConflicts(listOf("休"), null).isEmpty())
        assertTrue(repository.createDeck("Destination", listOf("休")).isEmpty())
        val source = repository.deckId("Source")
        val destination = repository.deckId("Destination")
        manager.writeTransaction { setOnlyAllowSingleDeckPerKanji(1L) }

        val conflicts = repository.getDeckConflicts(listOf("休", "国", "ひ"), destination)
        assertEquals(listOf("国"), conflicts.map { it.character })
        assertEquals(listOf(source), conflicts.single().decks.map { it.id })
        assertEquals(conflicts, repository.updateDeck(destination, "Changed", listOf("国", "ひ"), emptyList()))
        assertEquals("Destination", repository.getDeck(destination).name)
        assertEquals(listOf("休"), repository.getDeckCharacters(destination))

        val resolutions = mapOf("国" to MojiDeckConflictResolution.Leave)
        assertTrue(repository.updateDeck(destination, "Changed", listOf("休", "国", "ひ"), emptyList(), resolutions).isEmpty())
        assertEquals(setOf("休", "ひ"), repository.getDeckCharacters(destination).toSet())
        assertEquals(setOf("休", "国", "ひ"), repository.getDeckCharacters(source).toSet())

        manager.writeTransaction { setOnlyAllowSingleDeckPerKanji(0L) }
        assertTrue(repository.createDeck("Another", listOf("国"), resolutions).isEmpty())
        assertEquals(listOf("国"), repository.getDeckCharacters(repository.deckId("Another")))
    }

    @Test
    fun moveRequiresApprovalForEveryCurrentSourceAndKeepsReviewHistory() = withDatabase { manager, _ ->
        val repository = SqlDelightMojiPracticeRepository(manager)
        val history = SqlDelightReviewHistoryRepository(manager)
        repository.createDeck("First", listOf("国", "休"))
        repository.createDeck("Second", listOf("国"))
        val first = repository.deckId("First")
        val second = repository.deckId("Second")
        val review = ReviewHistoryItem("国", SrsPracticeType.MojiWriting.value, Instant.fromEpochMilliseconds(1000), 1.seconds, 3, 0, first)
        history.addReview(review)
        manager.writeTransaction { setOnlyAllowSingleDeckPerKanji(1L) }

        val staleResolutions = mapOf("国" to MojiDeckConflictResolution.Move(setOf(first)))
        val conflicts = repository.createDeck("Destination", listOf("国", "日"), staleResolutions)
        assertEquals("国", conflicts.single().character)
        assertEquals(setOf(first, second), conflicts.single().decks.map { it.id }.toSet())
        assertEquals(setOf("First", "Second"), repository.getDecks().map { it.name }.toSet())
        assertEquals(setOf("国", "休"), repository.getDeckCharacters(first).toSet())

        val resolutions = mapOf("国" to MojiDeckConflictResolution.Move(setOf(first, second)))
        assertTrue(repository.createDeck("Destination", listOf("国", "日"), resolutions).isEmpty())
        assertEquals(setOf("国", "日"), repository.getDeckCharacters(repository.deckId("Destination")).toSet())
        assertEquals(listOf("休"), repository.getDeckCharacters(first))
        assertTrue(repository.getDeckCharacters(second).isEmpty())
        assertEquals(listOf(review), history.getReviews(Instant.DISTANT_PAST, Instant.DISTANT_FUTURE))
    }

    @Test
    fun mergingChecksOnlyDecksOutsideTheMerge() = withDatabase { manager, _ ->
        val repository = SqlDelightMojiPracticeRepository(manager)
        repository.createDeck("First", listOf("人", "日", "休"))
        repository.createDeck("Second", listOf("人", "月"))
        repository.createDeck("Outside", listOf("日", "休", "国"))
        val selected = listOf(repository.deckId("First"), repository.deckId("Second"))
        val outside = repository.deckId("Outside")
        manager.writeTransaction { setOnlyAllowSingleDeckPerKanji(1L) }

        val conflicts = repository.createDeckAndMerge("Merged", selected)
        assertEquals(setOf("休", "日"), conflicts.map { it.character }.toSet())
        assertTrue(conflicts.all { it.decks.map { deck -> deck.id } == listOf(outside) })
        assertEquals(setOf("First", "Second", "Outside"), repository.getDecks().map { it.name }.toSet())

        val resolutions = mapOf(
            "休" to MojiDeckConflictResolution.Leave,
            "日" to MojiDeckConflictResolution.Move(setOf(outside))
        )
        assertTrue(repository.createDeckAndMerge("Merged", selected, resolutions).isEmpty())
        assertEquals(setOf("Outside", "Merged"), repository.getDecks().map { it.name }.toSet())
        assertEquals(setOf("人", "日", "月"), repository.getDeckCharacters(repository.deckId("Merged")).toSet())
        assertEquals(setOf("休", "国"), repository.getDeckCharacters(outside).toSet())
    }

    private suspend fun SqlDelightMojiPracticeRepository.deckId(name: String): Long =
        getDecks().single { it.name == name }.id

}
