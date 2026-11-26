package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.use_case

import ua.syt0r.kanji.core.user_data.database.MojiPracticeRepository
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksSortRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.MojiDashboardScreenContract

class MojiDashboardUpdateSortUseCase(
    private val appPreferences: PreferencesContract.AppPreferences,
    private val practiceRepository: MojiPracticeRepository
) : MojiDashboardScreenContract.UpdateSortUseCase {

    override suspend fun update(data: DecksSortRequestData) {
        appPreferences.mojiDashboardSortByTime.set(data.sortByTime)
        practiceRepository.updateDeckPositions(
            deckIdToPositionMap = data.reorderedList.reversed()
                .mapIndexed { index, item -> item.deckId to index }
                .toMap()
        )
    }

}