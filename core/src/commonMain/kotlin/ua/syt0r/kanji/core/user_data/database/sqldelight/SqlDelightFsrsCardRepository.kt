package ua.syt0r.kanji.core.user_data.database.sqldelight

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardParams
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus
import ua.syt0r.kanji.core.user_data.database.CachedUserDataState
import ua.syt0r.kanji.core.user_data.database.FsrsCardRepository
import ua.syt0r.kanji.core.user_data.database.ObservableRepository
import ua.syt0r.kanji.core.user_data.database.ObservableUserDataRepository
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.ReviewUndoToken
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.userdata.db.Fsrs_card
import ua.syt0r.kanji.core.userdata.db.UserDataQueries
import kotlin.time.Duration.Companion.milliseconds

class SqlDelightFsrsCardRepository private constructor(
    private val observableRepository: ObservableUserDataRepository,
    private val cachedUserDataState: CachedUserDataState<RepoData>
) : FsrsCardRepository,
    ObservableRepository by observableRepository,
    UserDataDatabaseContract.TransactionScope by observableRepository {

    constructor(
        userDataDatabaseManager: UserDataDatabaseContract.Manager
    ) : this(
        ObservableUserDataRepository(userDataDatabaseManager),
        CachedUserDataState(
            debugTitle = "fsrs_cards",
            resetFlow = userDataDatabaseManager.databaseChangeEvents,
            provider = { userDataDatabaseManager.readTransaction { loadRepoDataFromDB() } }
        )
    )

    private val writeMutex = Mutex()

    override suspend fun get(key: SrsCardKey): FsrsCard? {
        return getRepoData().cardsMap.value[key]
    }

    override suspend fun getAll(): Map<SrsCardKey, FsrsCard> {
        return getRepoData().cardsMap.value
    }

    override suspend fun update(key: SrsCardKey, card: FsrsCard) = writeMutex.withLock {
        val repoData = getRepoData()
        observableRepository.writeTransaction(
            onCommitted = { repoData.setCard(key, card) }
        ) {
            upsertFsrsCard(toRow(key, card))
        }
    }

    override suspend fun recordReview(
        key: SrsCardKey,
        card: FsrsCard,
        item: ReviewHistoryItem
    ): ReviewUndoToken = writeMutex.withLock {
        require(key.itemKey == item.key && key.practiceType == item.practiceType)
        val repoData = getRepoData()
        val updatedSchedule = toRow(key, card)
        observableRepository.writeTransaction(
            onCommitted = { repoData.setCard(key, card) }
        ) {
            val previousSchedule = getFsrsCard(key.itemKey, key.practiceType).executeAsOneOrNull()
            // The timestamp is part of the review's primary key.
            var timestamp = item.timestamp.toEpochMilliseconds()
            while (getReviewAt(key.itemKey, key.practiceType, timestamp).executeAsOneOrNull() != null) {
                timestamp++
            }
            val recordedReview = item.toRow().copy(timestamp = timestamp)
            upsertFsrsCard(updatedSchedule)
            upsertReview(recordedReview)
            ReviewUndoToken(
                previousSchedule = previousSchedule,
                updatedSchedule = updatedSchedule,
                recordedReview = recordedReview,
                totalReviews = getTotalReviewsCountOfType(key.itemKey, key.practiceType)
                    .executeAsOne().toInt()
            )
        }
    }

    override suspend fun undoReview(token: ReviewUndoToken): Boolean = writeMutex.withLock {
        val review = token.recordedReview
        val key = SrsCardKey(review.key, review.practice_type)
        val previousSchedule = token.previousSchedule
        val repoData = getRepoData()
        observableRepository.writeTransaction(
            onCommitted = { restored ->
                if (restored) {
                    repoData.setCard(key, previousSchedule?.convert())
                }
            }
        ) {
            val currentReview =
                getReviewAt(key.itemKey, key.practiceType, review.timestamp).executeAsOneOrNull()
            val currentSchedule = getFsrsCard(key.itemKey, key.practiceType).executeAsOneOrNull()
            if (currentReview != review || currentSchedule != token.updatedSchedule) {
                return@writeTransaction false
            }

            deleteReviewAt(key.itemKey, key.practiceType, review.timestamp)
            if (previousSchedule == null) deleteFsrsCard(key.itemKey, key.practiceType)
            else upsertFsrsCard(previousSchedule)
            true
        }
    }

    private suspend fun getRepoData(): RepoData {
        return cachedUserDataState.data.value.await()
    }

    private fun toRow(key: SrsCardKey, card: FsrsCard): Fsrs_card {
        card.params as FsrsCardParams.Existing
        return Fsrs_card(
            key = key.itemKey,
            practice_type = key.practiceType,
            status = card.status.ordinal.toLong(),
            stability = card.params.stability,
            difficulty = card.params.difficulty,
            lapses = card.lapses.toLong(),
            repeats = card.repeats.toLong(),
            last_review = card.lastReview!!.toEpochMilliseconds(),
            interval = card.interval.inWholeMilliseconds
        )
    }

    private class RepoData(cards: Map<SrsCardKey, FsrsCard>) {
        val cardsMap = MutableStateFlow(cards)

        fun setCard(key: SrsCardKey, card: FsrsCard?) {
            cardsMap.update { cards ->
                if (card == null) cards - key else cards + (key to card)
            }
        }
    }

    companion object {

        private val dbValueToSrcCardStatus: Map<Int, FsrsCardStatus> = FsrsCardStatus.entries
            .associateBy { it.ordinal }

        private fun UserDataQueries.loadRepoDataFromDB(): RepoData {
            val cardsMap = getFsrsCards().executeAsList()
                .associate { SrsCardKey(it.key, it.practice_type) to it.convert() }
            return RepoData(cardsMap)
        }

        private fun Fsrs_card.convert(): FsrsCard = FsrsCard(
            params = FsrsCardParams.Existing(
                difficulty = difficulty,
                stability = stability,
                reviewTime = Instant.fromEpochMilliseconds(last_review)
            ),
            status = dbValueToSrcCardStatus.getValue(status.toInt()),
            interval = interval.milliseconds,
            lapses = lapses.toInt(),
            repeats = repeats.toInt()
        )

    }

}
