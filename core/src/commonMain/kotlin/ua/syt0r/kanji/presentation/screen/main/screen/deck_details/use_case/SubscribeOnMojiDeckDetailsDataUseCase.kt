package ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.core.RefreshableData
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.refreshableDataFlow
import ua.syt0r.kanji.core.srs.MojiPracticeType
import ua.syt0r.kanji.core.srs.MojiSrsDeck
import ua.syt0r.kanji.core.srs.MojiSrsManager
import ua.syt0r.kanji.core.srs.SrsCardData
import ua.syt0r.kanji.presentation.LifecycleState
import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsItemData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.PracticeItemSummary
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTime

interface SubscribeOnDeckDetailsDataUseCase {
    operator fun invoke(
        configuration: DeckDetailsScreenConfiguration.MojiDeck,
        lifecycleState: StateFlow<LifecycleState>
    ): Flow<RefreshableData<DeckDetailsData.MojiDeckData>>
}

class DefaultSubscribeOnDeckDetailsDataUseCase(
    private val mojiSrsManager: MojiSrsManager,
    private val appDataRepository: AppDataRepository,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : SubscribeOnDeckDetailsDataUseCase {

    override operator fun invoke(
        configuration: DeckDetailsScreenConfiguration.MojiDeck,
        lifecycleState: StateFlow<LifecycleState>,
    ): Flow<RefreshableData<DeckDetailsData.MojiDeckData>> {
        val deckId = configuration.deckId
        return refreshableDataFlow(
            dataChangeFlow = mojiSrsManager.dataChangeFlow,
            lifecycleState = lifecycleState,
            valueProvider = {
                var data: DeckDetailsData.MojiDeckData
                val timeToRefreshData = measureTime { data = getUpdatedData(deckId) }
                Logger.d("timeToRefreshData[$timeToRefreshData]")
                data
            }
        )
    }

    private suspend fun getUpdatedData(
        deckId: Long
    ): DeckDetailsData.MojiDeckData = withContext(coroutineContext) {
        Logger.logMethod()

        val deck: MojiSrsDeck
        val writingMap: Map<String, SrsCardData>
        val readingMap: Map<String, SrsCardData>

        val timeToGetDeckInfo = measureTime {
            deck = mojiSrsManager.getDeck(deckId)
            writingMap = deck.progressMap.getValue(MojiPracticeType.Writing).itemsData
            readingMap = deck.progressMap.getValue(MojiPracticeType.Reading).itemsData
        }
        Logger.d("timeToGetDeckInfo[$timeToGetDeckInfo]")

        val timeZone = TimeZone.currentSystemDefault()

        val items = deck.items.mapIndexed { index, character ->

            val writingCardData = writingMap.getValue(character)
            val writingSummary = PracticeItemSummary(
                firstReviewDate = writingCardData.firstReview?.toLocalDateTime(timeZone),
                lastReviewDate = writingCardData.lastReview?.toLocalDateTime(timeZone),
                expectedReviewDate = writingCardData.expectedReviewDate,
                lapses = writingCardData.lapses,
                repeats = writingCardData.repeats,
                srsItemStatus = writingCardData.status
            )

            val readingCardData = readingMap.getValue(character)
            val readingSummary = PracticeItemSummary(
                firstReviewDate = readingCardData.firstReview?.toLocalDateTime(timeZone),
                lastReviewDate = readingCardData.lastReview?.toLocalDateTime(timeZone),
                expectedReviewDate = readingCardData.expectedReviewDate,
                lapses = readingCardData.lapses,
                repeats = readingCardData.repeats,
                srsItemStatus = readingCardData.status
            )

            DeckDetailsItemData.MojiData(
                character = character,
                positionInPractice = index,
                frequency = appDataRepository.getData(character)?.frequency,
                summaryMap = mapOf(
                    ScreenMojiPracticeType.Writing to writingSummary,
                    ScreenMojiPracticeType.Reading to readingSummary
                )
            )
        }

        DeckDetailsData.MojiDeckData(
            deckTitle = deck.title,
            items = items,
            sharableDeckData = items.joinToString("") { it.character }
        )

    }

}