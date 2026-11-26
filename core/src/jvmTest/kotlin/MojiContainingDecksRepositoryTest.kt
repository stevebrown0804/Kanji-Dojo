import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.SrsPracticeType
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.core.user_data.database.MojiDeckConflictResolution
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightMojiPracticeRepository
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightReviewHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MojiContainingDecksRepositoryTest {

    @Test
    fun returnsEveryContainingDeckInDisplayOrderRegardlessOfSingleDeckSetting() = withDatabase { manager, _ ->
        val repository = SqlDelightMojiPracticeRepository(manager)
        repository.createDeck("First", listOf("日", "日", "ひ"))
        repository.createDeck("Second", listOf("日"))
        repository.createDeck("Third", listOf("日"))
        repository.createDeck("Unrelated", listOf("本"))
        val first = repository.deckId("First")
        val second = repository.deckId("Second")
        val third = repository.deckId("Third")
        val unrelated = repository.deckId("Unrelated")
        repository.updateDeckPositions(mapOf(first to 2, second to 5, third to 5))

        assertTrue(repository.getDecksContainingMoji("月").isEmpty())
        assertEquals(listOf(unrelated), repository.getDecksContainingMoji("本").map { it.id })
        assertEquals(listOf(first), repository.getDecksContainingMoji("ひ").map { it.id })
        val expected = listOf(
            MojiDeck(second, "Second", 5),
            MojiDeck(third, "Third", 5),
            MojiDeck(first, "First", 2)
        )
        assertEquals(expected, repository.getDecksContainingMoji("日"))

        manager.writeTransaction { setOnlyAllowSingleDeckPerKanji(1L) }
        assertEquals(expected, repository.getDecksContainingMoji("日"))
        manager.writeTransaction { setOnlyAllowSingleDeckPerKanji(0L) }
        assertEquals(expected, repository.getDecksContainingMoji("日"))
    }

    @Test
    fun resultsFollowMembershipEditsMovesRenamesAndDeletion() = withDatabase { manager, _ ->
        val repository = SqlDelightMojiPracticeRepository(manager)
        repository.createDeck("First", listOf("日", "本"))
        repository.createDeck("Second", listOf("日"))
        repository.createDeck("Third", listOf("月"))
        repository.createDeck("Destination", emptyList())
        val first = repository.deckId("First")
        val second = repository.deckId("Second")
        val third = repository.deckId("Third")
        val destination = repository.deckId("Destination")

        repository.updateDeck(first, "First edited", emptyList(), listOf("日"))
        assertEquals(listOf(second), repository.getDecksContainingMoji("日").map { it.id })
        repository.updateDeck(third, "Third edited", listOf("日"), emptyList())
        assertEquals(listOf(third, second), repository.getDecksContainingMoji("日").map { it.id })

        manager.writeTransaction { setOnlyAllowSingleDeckPerKanji(1L) }
        val conflicts = repository.updateDeck(
            id = destination,
            title = "Destination",
            charactersToAdd = listOf("日"),
            charactersToRemove = emptyList(),
            conflictResolutions = mapOf("日" to MojiDeckConflictResolution.Move(setOf(second, third)))
        )
        assertTrue(conflicts.isEmpty())
        assertEquals(listOf(destination), repository.getDecksContainingMoji("日").map { it.id })

        repository.updateDeck(destination, "Renamed destination", emptyList(), emptyList())
        assertEquals("Renamed destination", repository.getDecksContainingMoji("日").single().name)
        repository.deleteDeck(destination)
        assertTrue(repository.getDecksContainingMoji("日").isEmpty())
        assertEquals(listOf(first), repository.getDecksContainingMoji("本").map { it.id })
    }

    @Test
    fun readsMembershipsUsingOriginalDatabaseTableNames() = withDatabase { manager, driver ->
        driver.execute(null, "INSERT INTO letter_deck (id, name, position) VALUES (41, 'Imported deck', 7)", 0)
        driver.execute(null, "INSERT INTO letter_deck_entry (character, deck_id) VALUES ('日', 41)", 0)

        val repository = SqlDelightMojiPracticeRepository(manager)
        assertEquals(listOf(MojiDeck(41, "Imported deck", 7)), repository.getDecksContainingMoji("日"))
    }

    @Test
    fun removesOnlyTheSelectedMembershipAndKeepsReviewHistory() = withDatabase { manager, _ ->
        val repository = SqlDelightMojiPracticeRepository(manager)
        val history = SqlDelightReviewHistoryRepository(manager)
        repository.createDeck("First", listOf("日", "本"))
        repository.createDeck("Second", listOf("日", "月"))
        val first = repository.deckId("First")
        val second = repository.deckId("Second")
        val originalDecks = repository.getDecks()
        val review = ReviewHistoryItem(
            key = "日",
            practiceType = SrsPracticeType.MojiWriting.value,
            timestamp = Instant.fromEpochMilliseconds(1000),
            duration = 1.seconds,
            grade = 3,
            mistakes = 0,
            deckId = first
        )
        history.addReview(review)

        assertTrue(repository.tryRemoveMojiFromDeck("日", first, allowLastDeckRemoval = false))

        assertEquals(originalDecks, repository.getDecks())
        assertEquals(listOf("本"), repository.getDeckCharacters(first))
        assertEquals(setOf("日", "月"), repository.getDeckCharacters(second).toSet())
        assertEquals(listOf(second), repository.getDecksContainingMoji("日").map { it.id })
        assertEquals(listOf(review), history.getReviews(Instant.DISTANT_PAST, Instant.DISTANT_FUTURE))
    }

    @Test
    fun checksCurrentLastMembershipAndHandlesRepeatedRemovals() = withDatabase { manager, _ ->
        val repository = SqlDelightMojiPracticeRepository(manager)
        repository.createDeck("Selected", listOf("日"))
        repository.createDeck("Other", listOf("日"))
        val selected = repository.deckId("Selected")
        val other = repository.deckId("Other")

        assertEquals(2, repository.getDecksContainingMoji("日").size)
        assertTrue(repository.tryRemoveMojiFromDeck("日", other, allowLastDeckRemoval = false))

        assertFalse(repository.tryRemoveMojiFromDeck("日", selected, allowLastDeckRemoval = false))
        assertTrue(repository.tryRemoveMojiFromDeck("日", other, allowLastDeckRemoval = false))
        assertEquals(listOf(selected), repository.getDecksContainingMoji("日").map { it.id })

        assertTrue(repository.tryRemoveMojiFromDeck("日", selected, allowLastDeckRemoval = true))
        assertTrue(repository.tryRemoveMojiFromDeck("日", selected, allowLastDeckRemoval = false))
        assertTrue(repository.getDecksContainingMoji("日").isEmpty())
        assertEquals("Selected", repository.getDeck(selected).name)
    }

    private suspend fun SqlDelightMojiPracticeRepository.deckId(name: String): Long =
        getDecks().single { it.name == name }.id
}
