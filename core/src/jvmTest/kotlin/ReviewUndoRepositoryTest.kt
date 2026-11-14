import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import ua.syt0r.kanji.core.logger.LoggerConfiguration
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardParams
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.user_data.database.UserDatabaseInfo
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightFsrsCardRepository
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.db.UserDataDatabase
import ua.syt0r.kanji.core.userdata.db.UserDataQueries
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class ReviewUndoRepositoryTest {

    @Test
    fun undoRestoresThePriorScheduleAndPreservesExistingReviews() = withDatabase { manager, _ ->
        val cards = SqlDelightFsrsCardRepository(manager)
        val history = SqlDelightReviewHistoryRepository(manager)
        val priorReview = review(1_000, grade = 3)
        val readingReview = priorReview.copy(practiceType = LetterPracticeType.Reading.toSrsKey(key.itemKey).practiceType)
        cards.update(key, priorSchedule)
        history.addReview(priorReview)
        history.addReview(readingReview)

        val token = cards.recordReview(key, changedSchedule, review(1_000, grade = 1))
        assertEquals(2, token.totalReviews)
        assertEquals(changedSchedule, cards.get(key))
        assertEquals(2L, history.getTotalReviewCount(key.itemKey, key.practiceType))

        assertTrue(cards.undoReview(token))
        assertEquals(priorSchedule, cards.get(key))
        assertEquals(priorSchedule, SqlDelightFsrsCardRepository(manager).get(key))
        assertEquals(setOf(priorReview, readingReview), history.allReviews().toSet())
    }

    @Test
    fun undoRemovesTheScheduleCreatedByTheFirstGrade() = withDatabase { manager, _ ->
        val cards = SqlDelightFsrsCardRepository(manager)
        val history = SqlDelightReviewHistoryRepository(manager)
        val token = cards.recordReview(key, changedSchedule, review(2_000, grade = 1))
        assertEquals(1, token.totalReviews)

        assertTrue(cards.undoReview(token))
        assertNull(cards.get(key))
        assertNull(SqlDelightFsrsCardRepository(manager).get(key))
        assertTrue(history.allReviews().isEmpty())
    }

    @Test
    fun undoKeepsNewerGradesUntilTheyAreUndoneInOrder() = withDatabase { manager, _ ->
        val cards = SqlDelightFsrsCardRepository(manager)
        val history = SqlDelightReviewHistoryRepository(manager)
        val firstReview = review(1_000, grade = 3)
        val secondReview = review(2_000, grade = 1)
        val first = cards.recordReview(key, priorSchedule, firstReview)
        val second = cards.recordReview(key, changedSchedule, secondReview)

        assertFalse(cards.undoReview(first))
        assertEquals(changedSchedule, cards.get(key))
        assertEquals(setOf(firstReview, secondReview), history.allReviews().toSet())

        assertTrue(cards.undoReview(second))
        assertEquals(priorSchedule, cards.get(key))
        assertEquals(listOf(firstReview), history.allReviews())
        assertTrue(cards.undoReview(first))
        assertNull(cards.get(key))
        assertTrue(history.allReviews().isEmpty())
    }

    @Test
    fun failedScheduleRestoreLeavesTheGradeAndScheduleIntact() = withDatabase { manager, driver ->
        val cards = SqlDelightFsrsCardRepository(manager)
        val history = SqlDelightReviewHistoryRepository(manager)
        val savedReview = review(2_000, grade = 1)
        cards.update(key, priorSchedule)
        val token = cards.recordReview(key, changedSchedule, savedReview)
        driver.execute(
            null,
            """
            CREATE TRIGGER reject_prior_schedule
            BEFORE INSERT ON fsrs_card
            WHEN NEW.last_review = 1000
            BEGIN
                SELECT RAISE(ABORT, 'Schedule restore rejected');
            END
            """.trimIndent(),
            0
        )

        assertFailsWith<SQLException> { cards.undoReview(token) }
        assertEquals(changedSchedule, cards.get(key))
        assertEquals(changedSchedule, SqlDelightFsrsCardRepository(manager).get(key))
        assertEquals(listOf(savedReview), history.allReviews())
    }

    private val key = LetterPracticeType.Writing.toSrsKey("休")
    private val priorSchedule = FsrsCard(
        status = FsrsCardStatus.Review,
        params = FsrsCardParams.Existing(
            difficulty = 3.5,
            stability = 2.5,
            reviewTime = Instant.fromEpochMilliseconds(1_000)
        ),
        interval = 2.days,
        lapses = 1,
        repeats = 4
    )
    private val changedSchedule = FsrsCard(
        status = FsrsCardStatus.Relearning,
        params = FsrsCardParams.Existing(
            difficulty = 4.5,
            stability = 1.5,
            reviewTime = Instant.fromEpochMilliseconds(2_000)
        ),
        interval = 60.seconds,
        lapses = 2,
        repeats = 5
    )

    private fun review(timestamp: Long, grade: Int) = ReviewHistoryItem(
        key = key.itemKey,
        practiceType = key.practiceType,
        timestamp = Instant.fromEpochMilliseconds(timestamp),
        duration = 5.seconds,
        grade = grade,
        mistakes = 0,
        deckId = 1
    )

    private suspend fun SqlDelightReviewHistoryRepository.allReviews() =
        getReviews(Instant.DISTANT_PAST, Instant.DISTANT_FUTURE)

}

internal fun withDatabase(
    block: suspend (UserDataDatabaseContract.Manager, JdbcSqliteDriver) -> Unit
) = runBlocking {
    startKoin { modules(module { single { LoggerConfiguration(isEnabled = false) } }) }
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
        UserDataDatabase.Schema.create(driver).value
        block(TestDatabaseManager(UserDataDatabase(driver).userDataQueries), driver)
    } finally {
        driver.close()
        stopKoin()
    }
}

private class TestDatabaseManager(
    private val queries: UserDataQueries
) : UserDataDatabaseContract.Manager {
    override val databaseChangeEvents = MutableSharedFlow<Unit>()

    override suspend fun <T> readTransaction(block: UserDataQueries.() -> T): T =
        queries.transactionWithResult { block(queries) }

    override suspend fun <T> writeTransaction(block: UserDataQueries.() -> T): T =
        queries.transactionWithResult { block(queries) }

    override suspend fun withDisconnectedDatabase(scope: suspend (UserDatabaseInfo) -> Unit) =
        error("This test database cannot disconnect.")

    override suspend fun replaceDatabase(byteReadChannel: ByteReadChannel) =
        error("This test database cannot be replaced.")
}
