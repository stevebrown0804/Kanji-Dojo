package ua.syt0r.kanji.core.user_data.database

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import kotlin.time.Duration

interface ObservableRepository {
    val changesFlow: SharedFlow<Unit>
}

interface MojiPracticeRepository : ObservableRepository {
    suspend fun createDeck(
        title: String,
        characters: List<String>,
        conflictResolutions: Map<String, MojiDeckConflictResolution> = emptyMap(),
    ): List<MojiDeckConflict>

    suspend fun createDeckAndMerge(
        title: String,
        deckIdToMerge: List<Long>,
        conflictResolutions: Map<String, MojiDeckConflictResolution> = emptyMap(),
    ): List<MojiDeckConflict>

    suspend fun updateDeckPositions(deckIdToPositionMap: Map<Long, Int>)
    suspend fun deleteDeck(id: Long)
    suspend fun updateDeck(
        id: Long,
        title: String,
        charactersToAdd: List<String>,
        charactersToRemove: List<String>,
        conflictResolutions: Map<String, MojiDeckConflictResolution> = emptyMap(),
    ): List<MojiDeckConflict>

    /**
     * Returns true when the moji is absent from the selected deck after the operation.
     * Returns false when removing its last membership requires confirmation.
     */
    suspend fun tryRemoveMojiFromDeck(
        character: String,
        deckId: Long,
        allowLastDeckRemoval: Boolean,
    ): Boolean

    suspend fun getDecks(): List<MojiDeck>
    suspend fun getDecksContainingMoji(character: String): List<MojiDeck>
    suspend fun getDeck(id: Long): MojiDeck
    suspend fun getDeckCharacters(id: Long): List<String>
    suspend fun getDeckConflicts(
        characters: List<String>,
        targetDeckId: Long?,
    ): List<MojiDeckConflict>
}

interface VocabPracticeRepository : ObservableRepository {
    suspend fun createDeck(title: String, words: List<VocabCardData>)
    suspend fun mergeDecks(newDeckTitle: String, deckIdToMerge: List<Long>)
    suspend fun updateDeckPositions(deckIdToPositionMap: Map<Long, Int>)
    suspend fun deleteDeck(id: Long)
    suspend fun getDecks(): List<VocabDeck>
    suspend fun getDecksContainingWord(kanji: String?, kana: String): List<Long>

    suspend fun updateDeck(
        id: Long,
        title: String,
        cardsToAdd: List<VocabCardData>,
        cardsToUpdate: List<SavedVocabCard>,
        cardsToRemove: List<Long>
    )

    suspend fun addCard(deckId: Long, data: VocabCardData)
    suspend fun deleteCard(id: Long)
    suspend fun getCardIdList(deckId: Long): List<Long>
    suspend fun getAllCards(): List<SavedVocabCard>
}

interface FsrsCardRepository : ObservableRepository {
    suspend fun get(key: SrsCardKey): FsrsCard?
    suspend fun getAll(): Map<SrsCardKey, FsrsCard>
    suspend fun update(key: SrsCardKey, card: FsrsCard)
    suspend fun recordReview(key: SrsCardKey, card: FsrsCard, item: ReviewHistoryItem): ReviewUndoToken
    suspend fun undoReview(token: ReviewUndoToken): Boolean
}

interface ReviewHistoryRepository {
    suspend fun addReview(item: ReviewHistoryItem)
    suspend fun getReviews(start: Instant, end: Instant): List<ReviewHistoryItem>
    suspend fun getFirstReviewTime(key: String, practiceType: Long): Instant?
    suspend fun getDeckLastReview(deckId: Long, practiceTypes: List<Long>): Instant?
    suspend fun getTotalReviewsCount(): Long
    suspend fun getTotalReviewCount(key: String, practiceType: Long): Long
    suspend fun getUniqueReviewItemsCount(practiceTypes: List<Long>): Long
    suspend fun getTotalPracticeTime(singleReviewDurationLimit: Long): Duration
    suspend fun getStreaks(timeOffset: LocalTime): List<StreakData>
    suspend fun getReviewHistoryStatsForKeys(
        keys: List<String>
    ): Map<String, ReviewHistoryStatItem>
}
