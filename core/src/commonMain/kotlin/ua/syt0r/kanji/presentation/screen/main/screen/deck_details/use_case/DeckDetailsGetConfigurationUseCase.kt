package ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case

import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.common.ScreenVocabPracticeType
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.FilterConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.toScreenType

interface DeckDetailsGetConfigurationUseCase {
    suspend fun mojiConfiguration(): DeckDetailsConfiguration.MojiDeckConfiguration
    suspend fun vocabConfiguration(): DeckDetailsConfiguration.VocabDeckConfiguration
}

class DefaultDeckDetailsGetConfigurationUseCase(
    private val repository: PreferencesContract.AppPreferences,
) : DeckDetailsGetConfigurationUseCase {

    override suspend fun mojiConfiguration(): DeckDetailsConfiguration.MojiDeckConfiguration {
        return repository.run {
            DeckDetailsConfiguration.MojiDeckConfiguration(
                practiceType = ScreenMojiPracticeType.from(practiceType.get()),
                filterConfiguration = FilterConfiguration(
                    showNew = filterNew.get(),
                    showDue = filterDue.get(),
                    showDone = filterDone.get(),
                ),
                sortOption = sortOption.get().toScreenType(),
                isDescending = isSortDescending.get(),
                layout = practicePreviewLayout.get().toScreenType(),
                kanaGroups = kanaGroupsEnabled.get(),
            )
        }
    }

    override suspend fun vocabConfiguration(): DeckDetailsConfiguration.VocabDeckConfiguration {
        return repository.run {
            DeckDetailsConfiguration.VocabDeckConfiguration(
                practiceType = ScreenVocabPracticeType.Flashcard,
                filterConfiguration = FilterConfiguration(
                    showNew = true,
                    showDue = true,
                    showDone = true
                )
            )
        }
    }

}