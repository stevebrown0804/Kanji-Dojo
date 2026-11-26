package ua.syt0r.kanji.core.srs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.core.user_data.database.MojiPracticeRepository
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

interface MojiSrsManager {
    val dataChangeFlow: SharedFlow<Unit>
    suspend fun getDecks(): MojiSrsDecksData
    suspend fun getDeck(deckId: Long): MojiSrsDeck
}

typealias MojiSrsDecksData = SrsDecksData<MojiSrsDeck, MojiPracticeType>
typealias MojiSrsDeckDescriptor = SrsDeckDescriptor<String, MojiPracticeType>
typealias MojiSrsDeckProgress = SrsDeckProgress<String>

data class MojiSrsDeck(
    override val id: Long,
    override val title: String,
    override val position: Int,
    override val items: List<String>,
    override val lastReview: Instant?,
    override val progressMap: Map<MojiPracticeType, MojiSrsDeckProgress>
) : SrsDeckData<MojiPracticeType, String>

class DefaultMojiSrsManager(
    private val practiceRepository: MojiPracticeRepository,
    private val srsCardRepository: SrsCardRepository,
    dailyLimitManager: DailyLimitManager,
    timeUtils: TimeUtils,
    private val appPreferences: PreferencesContract.AppPreferences,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    coroutineScope: CoroutineScope
) : SrsManager<String, MojiPracticeType, MojiSrsDeck>(
    deckChangesFlow = practiceRepository.changesFlow,
    srsChangesFlow = srsCardRepository.changesFlow,
    dailyLimitManager = dailyLimitManager,
    timeUtils = timeUtils,
    appPreferences = appPreferences,
    coroutineScope = coroutineScope
), MojiSrsManager {

    override val practiceTypes: List<MojiPracticeType> = MojiPracticeType.entries

    override suspend fun getDecks(): MojiSrsDecksData {
        return getDecksInternal()
    }

    override suspend fun getDeck(deckId: Long): MojiSrsDeck {
        return getDecksInternal().decks.first { it.id == deckId }
    }

    override suspend fun getDeckDescriptors(): List<MojiSrsDeckDescriptor> {
        return practiceRepository.getDecks().map { getDeckDescriptor(it) }
    }

    private suspend fun getDeckDescriptor(it: MojiDeck): MojiSrsDeckDescriptor {
        val items = practiceRepository.getDeckCharacters(it.id)
        val historyStats = reviewHistoryRepository
            .getReviewHistoryStatsForKeys(keys = items.map { it })

        val itemsDataMap = MojiPracticeType.entries.associateWith { practiceType ->
            val itemsData: Map<String, SrsCardData> = items.associateWith { moji ->
                val key = practiceType.toSrsKey(moji)
                val card = srsCardRepository.get(key)
                val itemReviewStats = historyStats[moji]
                val practiceTypeStat = itemReviewStats?.practiceTypeToDataMap
                    ?.get(practiceType.srsPracticeType.value)
                SrsCardData(
                    key = key,
                    card = card,
                    status = getSrsStatus(card),
                    lapses = card?.fsrsCard?.lapses ?: 0,
                    repeats = card?.fsrsCard?.repeats ?: 0,
                    firstReview = practiceTypeStat?.firstReview,
                    firstReviewSrsDate = practiceTypeStat?.firstReview?.toSrsDate(),
                    lastReview = card?.lastReview,
                    lastReviewSrsDate = card?.lastReview?.toSrsDate(),
                    expectedReviewDate = card?.expectedReview?.toSrsDate()
                )
            }
            PracticeTypeDeckData(itemsData = itemsData)
        }

        return MojiSrsDeckDescriptor(
            id = it.id,
            title = it.name,
            position = it.position,
            lastReview = historyStats
                .flatMap { it.value.practiceTypeToDataMap.values }
                .maxOfOrNull { it.lastReview },
            items = items,
            itemsData = itemsDataMap
        )
    }

    override suspend fun getDeckSortConfiguration(): DeckSortConfiguration {
        return DeckSortConfiguration(
            sortByReviewDate = appPreferences.mojiDashboardSortByTime.get()
        )
    }

    override suspend fun getDeckLimit(
        configuration: DailyLimitConfiguration,
        newDoneToday: Int,
        dueDoneToday: Int
    ): DeckLimit.EnabledDeckLimit {
        return when {
            configuration.isMojiLimitCombined -> DeckLimit.Combined(
                limit = configuration.mojiCombinedLimit,
                newDone = newDoneToday,
                dueDone = dueDoneToday
            )

            else -> DeckLimit.Separate(
                limitsMap = configuration.mojiSeparatedLimit
            )
        }
    }

    override suspend fun createDeck(
        deckDescriptor: MojiSrsDeckDescriptor,
        deckLimit: DeckLimit,
        currentSrsDate: LocalDate
    ): MojiSrsDeck {
        return MojiSrsDeck(
            id = deckDescriptor.id,
            title = deckDescriptor.title,
            position = deckDescriptor.position,
            lastReview = deckDescriptor.lastReview,
            items = deckDescriptor.items,
            progressMap = deckDescriptor.itemsData.mapValues { (practiceType, deckData) ->
                deckData.toProgress(deckLimit, practiceType, currentSrsDate)
            }
        )
    }

}
