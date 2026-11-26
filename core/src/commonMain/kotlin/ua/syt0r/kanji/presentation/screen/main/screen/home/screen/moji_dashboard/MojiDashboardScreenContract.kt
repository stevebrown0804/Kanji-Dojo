package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.RefreshableData
import ua.syt0r.kanji.core.user_data.database.MojiDeckConflict
import ua.syt0r.kanji.core.user_data.database.MojiDeckConflictResolution
import ua.syt0r.kanji.presentation.LifecycleState
import ua.syt0r.kanji.presentation.dialog.MojiDeckConflictPrompt
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardListState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksMergeRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksSortRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.MojiDeckDashboardPracticeTypeItem

interface MojiDashboardScreenContract {

    interface ViewModel {

        val state: State<ScreenState>
        val mojiDeckConflictPrompt: MojiDeckConflictPrompt

        fun mergeDecks(data: DecksMergeRequestData)
        fun sortDecks(data: DecksSortRequestData)

    }

    sealed class ScreenState {

        object Loading : ScreenState()

        data class Loaded(
            val listState: DeckDashboardListState,
            val practiceTypeItems: List<MojiDeckDashboardPracticeTypeItem>,
            val selectedPracticeTypeItem: MutableState<MojiDeckDashboardPracticeTypeItem>
        ) : ScreenState()

    }

    interface LoadDataUseCase {
        fun load(
            lifecycleState: StateFlow<LifecycleState>
        ): Flow<RefreshableData<MojiDashboardScreenData>>
    }

    interface MergeDecksUseCase {
        suspend operator fun invoke(
            data: DecksMergeRequestData,
            conflictResolutions: Map<String, MojiDeckConflictResolution>,
        ): List<MojiDeckConflict>
    }

    interface UpdateSortUseCase {
        suspend fun update(data: DecksSortRequestData)
    }

}
