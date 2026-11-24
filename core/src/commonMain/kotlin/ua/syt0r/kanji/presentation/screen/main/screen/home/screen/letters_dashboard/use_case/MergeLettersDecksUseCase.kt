package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.use_case

import ua.syt0r.kanji.core.user_data.database.LetterDeckConflict
import ua.syt0r.kanji.core.user_data.database.LetterDeckConflictResolution
import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksMergeRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.LettersDashboardScreenContract

class MergeLettersDecksUseCase(
    private val repository: LetterPracticeRepository
) : LettersDashboardScreenContract.MergeDecksUseCase {

    override suspend operator fun invoke(
        data: DecksMergeRequestData,
        conflictResolutions: Map<String, LetterDeckConflictResolution>,
    ): List<LetterDeckConflict> = repository.createDeckAndMerge(
        title = data.title,
        deckIdToMerge = data.deckIds,
        conflictResolutions = conflictResolutions,
    )

}
