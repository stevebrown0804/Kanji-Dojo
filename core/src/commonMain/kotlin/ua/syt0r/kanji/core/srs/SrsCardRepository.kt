package ua.syt0r.kanji.core.srs

import ua.syt0r.kanji.core.user_data.database.FsrsCardRepository
import ua.syt0r.kanji.core.user_data.database.ObservableRepository
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem
import ua.syt0r.kanji.core.user_data.database.ReviewUndoToken

interface SrsCardRepository : ObservableRepository {
    suspend fun get(key: SrsCardKey): SrsCard?
    suspend fun getAll(): Map<SrsCardKey, SrsCard>
    suspend fun update(key: SrsCardKey, card: SrsCard)
    suspend fun recordReview(key: SrsCardKey, card: SrsCard, item: ReviewHistoryItem): ReviewUndoToken
    suspend fun undoReview(token: ReviewUndoToken): Boolean
}

class DefaultSrsCardRepository(
    private val fsrsCardRepository: FsrsCardRepository
) : SrsCardRepository,
    ObservableRepository by fsrsCardRepository {

    override suspend fun get(key: SrsCardKey): SrsCard? {
        return fsrsCardRepository.get(key)?.let { SrsCard(it) }
    }

    override suspend fun getAll(): Map<SrsCardKey, SrsCard> {
        return fsrsCardRepository.getAll().mapValues { SrsCard(it.value) }
    }

    override suspend fun update(key: SrsCardKey, card: SrsCard) {
        fsrsCardRepository.update(key, card.fsrsCard)
    }

    override suspend fun recordReview(
        key: SrsCardKey,
        card: SrsCard,
        item: ReviewHistoryItem
    ): ReviewUndoToken = fsrsCardRepository.recordReview(key, card.fsrsCard, item)

    override suspend fun undoReview(token: ReviewUndoToken): Boolean =
        fsrsCardRepository.undoReview(token)

}
