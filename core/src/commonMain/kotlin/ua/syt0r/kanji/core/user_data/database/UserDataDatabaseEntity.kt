package ua.syt0r.kanji.core.user_data.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import ua.syt0r.kanji.core.file.PlatformFile
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.user_data.db.UserDataDatabase
import ua.syt0r.kanji.core.userdata.db.Fsrs_card
import ua.syt0r.kanji.core.userdata.db.Review_history
import kotlin.time.Duration


data class DatabaseConnection(
    val sqlDriver: SqlDriver,
    val database: UserDataDatabase
) {
    fun closeConnection() = sqlDriver.close()
}

sealed interface DatabaseMigrationState {
    data object Idle : DatabaseMigrationState

    data class Running(
        val message: String,
        val progress: Progress? = null
    ) : DatabaseMigrationState {
        data class Progress(val current: Int, val total: Int)
    }
}

class UserDatabaseInfo(
    val version: Long,
    val file: PlatformFile
)


data class LetterDeck(
    val id: Long,
    val name: String,
    val position: Int,
)

data class CharacterStudyProgress(
    val character: String,
    val practiceType: LetterPracticeType,
    val lastReviewTime: Instant,
    val repeats: Int,
    val lapses: Int,
)


data class VocabDeck(
    val id: Long,
    val title: String,
    val position: Int
)

data class VocabCardData(
    val kanjiReading: String?,
    val kanaReading: String,
    val meaning: String?,
    val dictionaryId: Long?
)

data class SavedVocabCard(
    val cardId: Long,
    val deckId: Long,
    val data: VocabCardData
)


data class ReviewHistoryItem(
    val key: String,
    val practiceType: Long,
    val timestamp: Instant,
    val duration: Duration,
    val grade: Int,
    val mistakes: Int,
    val deckId: Long,
)

class ReviewUndoToken internal constructor(
    internal val previousSchedule: Fsrs_card?,
    internal val updatedSchedule: Fsrs_card,
    internal val recordedReview: Review_history,
    val totalReviews: Int
)

data class ReviewHistoryStatItem(
    val key: String,
    val practiceTypeToDataMap: Map<Long, PracticeTypeData>
) {
    data class PracticeTypeData(val firstReview: Instant, val lastReview: Instant)
}

class StreakData(
    val start: LocalDate,
    val end: LocalDate,
    val length: Int
)
