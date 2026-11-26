package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.use_case

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.RefreshableData
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.refreshableDataFlow
import ua.syt0r.kanji.core.srs.MojiPracticeType
import ua.syt0r.kanji.core.srs.MojiSrsDeckProgress
import ua.syt0r.kanji.core.srs.MojiSrsManager
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.presentation.LifecycleState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.MojiDeckDashboardItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.MojiDeckStudyProgress
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.MojiDashboardScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.MojiDashboardScreenData

class MojiDashboardLoadDataUseCase(
    private val srsManager: MojiSrsManager,
    private val timeUtils: TimeUtils,
) : MojiDashboardScreenContract.LoadDataUseCase {

    override fun load(
        lifecycleState: StateFlow<LifecycleState>
    ): Flow<RefreshableData<MojiDashboardScreenData>> {
        return refreshableDataFlow(
            dataChangeFlow = srsManager.dataChangeFlow,
            lifecycleState = lifecycleState,
            valueProvider = { getUpdatedScreenData() }
        )
    }

    private suspend fun getUpdatedScreenData(): MojiDashboardScreenData {
        Logger.logMethod()
        val srsDecksData = srsManager.getDecks()
        val currentInstant = timeUtils.now()
        return MojiDashboardScreenData(
            items = srsDecksData.decks.map { deck ->
                val writingProgress = deck.progressMap
                    .getValue(MojiPracticeType.Writing)
                    .toPracticeStudyProgress()
                val readingProgress = deck.progressMap
                    .getValue(MojiPracticeType.Reading)
                    .toPracticeStudyProgress()
                MojiDeckDashboardItem(
                    deckId = deck.id,
                    title = deck.title,
                    position = deck.position,
                    elapsedSinceLastReview = deck.lastReview?.let { currentInstant - it },
                    writingProgress = writingProgress,
                    readingProgress = readingProgress
                )
            },
            dailyLimitEnabled = srsDecksData.dailyLimitEnabled
        )
    }

    private fun MojiSrsDeckProgress.toPracticeStudyProgress(): MojiDeckStudyProgress {
        return MojiDeckStudyProgress(
            all = itemsData.keys.toList(),
            completed = done,
            due = due,
            new = new,
            dailyNew = dailyNew,
            dailyDue = dailyDue
        )
    }

}
