package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case

import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeConfiguration

interface UpdateMojiPracticeConfigurationUseCase {
    suspend operator fun invoke(configuration: MojiPracticeConfiguration)
}

class DefaultUpdateMojiPracticeConfigurationUseCase(
    private val practicePreferences: PreferencesContract.PracticePreferences
) : UpdateMojiPracticeConfigurationUseCase {

    override suspend fun invoke(
        configuration: MojiPracticeConfiguration
    ) = practicePreferences.run {
        shuffle.set(configuration.selectorState.shuffle.value)
        newCardsOrder.set(configuration.selectorState.newCardsOrder.value)

        when (configuration) {
            is MojiPracticeConfiguration.Writing -> {
                noTranslationLayout.set(configuration.noTranslationsLayout.value)
                leftHandMode.set(configuration.leftHandedMode.value)
                writingRomajiInsteadOfKanaWords.set(configuration.useRomajiForKanaWords.value)
                writingInputMethod.set(configuration.inputMode.value.repoType)
                altStrokeEvaluator.set(configuration.altStrokeEvaluatorEnabled.value)
            }

            is MojiPracticeConfiguration.Reading -> {
                readingRomajiFuriganaForKanaWords.set(configuration.useRomajiForKanaWords.value)
            }
        }
    }
}