package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.use_case

import ua.syt0r.kanji.core.user_data.database.MojiDeckConflict
import ua.syt0r.kanji.core.user_data.database.MojiDeckConflictResolution
import ua.syt0r.kanji.core.user_data.database.MojiPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksMergeRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.MojiDashboardScreenContract

class MergeMojiDecksUseCase(
    private val repository: MojiPracticeRepository
) : MojiDashboardScreenContract.MergeDecksUseCase {

    override suspend operator fun invoke(
        data: DecksMergeRequestData,
        conflictResolutions: Map<String, MojiDeckConflictResolution>,
    ): List<MojiDeckConflict> = repository.createDeckAndMerge(
        title = data.title,
        deckIdToMerge = data.deckIds,
        conflictResolutions = conflictResolutions,
    )

}
