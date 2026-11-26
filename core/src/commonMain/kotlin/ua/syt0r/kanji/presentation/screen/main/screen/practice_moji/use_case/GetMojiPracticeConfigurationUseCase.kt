package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesNewCardsOrder
import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeConfigurationCardsSelectorState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.WritingPracticeHintMode
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.toScreenType

interface GetMojiPracticeConfigurationUseCase {
    suspend operator fun invoke(
        configuration: MojiPracticeScreenConfiguration
    ): MojiPracticeConfiguration
}

data class MojiPracticeCardConfigurationData(
    val moji: String,
    val deckId: Long,
    val srsCardKey: SrsCardKey,
    val isNew: Boolean
)

class DefaultGetMojiPracticeConfigurationUseCase(
    private val practicePreferences: PreferencesContract.PracticePreferences,
    private val srsCardRepository: SrsCardRepository
) : GetMojiPracticeConfigurationUseCase {

    override suspend fun invoke(configuration: MojiPracticeScreenConfiguration): MojiPracticeConfiguration {
        val srsCardCache = srsCardRepository.getAll()

        val defaultCardsOrder = configuration.cards.toList().map {
            val srsKey = configuration.practiceType.dataType.toSrsKey(it.moji)
            MojiPracticeCardConfigurationData(
                moji = it.moji,
                deckId = it.deckId,
                srsCardKey = srsKey,
                isNew = srsCardCache.contains(srsKey).not()
            )
        }

        val newCards = defaultCardsOrder.filter { it.isNew }
        val nonNewCards = defaultCardsOrder.filterNot { it.isNew }

        val selectorState = PracticeConfigurationCardsSelectorState(
            cardsCount = configuration.cards.size,
            shuffle = mutableStateOf(practicePreferences.shuffle.get()),
            newCardsOrder = mutableStateOf(practicePreferences.newCardsOrder.get())
        )

        val unfilteredResultCardsList = derivedStateOf {
            val shuffle = selectorState.shuffle.value
            val newCardsOrder = selectorState.newCardsOrder.value

            fun <T> List<T>.withShuffleApplied(): List<T> =
                if (shuffle) shuffled() else this

            when (newCardsOrder) {
                PreferencesNewCardsOrder.First -> newCards.withShuffleApplied()
                    .plus(nonNewCards.withShuffleApplied())

                PreferencesNewCardsOrder.Last -> nonNewCards.withShuffleApplied()
                    .plus(newCards.withShuffleApplied())

                PreferencesNewCardsOrder.Mixed -> defaultCardsOrder.withShuffleApplied()
            }
        }

        return when (configuration.practiceType) {
            ScreenMojiPracticeType.Writing -> {
                MojiPracticeConfiguration.Writing(
                    selectorState = selectorState,
                    unfilteredResultCardsList = unfilteredResultCardsList,
                    noTranslationsLayout = mutableStateOf(practicePreferences.noTranslationLayout.get()),
                    leftHandedMode = mutableStateOf(practicePreferences.leftHandMode.get()),
                    useRomajiForKanaWords = mutableStateOf(practicePreferences.writingRomajiInsteadOfKanaWords.get()),
                    inputMode = mutableStateOf(
                        practicePreferences.writingInputMethod.get().toScreenType()
                    ),
                    hintMode = mutableStateOf(WritingPracticeHintMode.OnlyNew),
                    altStrokeEvaluatorEnabled = mutableStateOf(practicePreferences.altStrokeEvaluator.get())
                )
            }

            ScreenMojiPracticeType.Reading -> {
                MojiPracticeConfiguration.Reading(
                    selectorState = selectorState,
                    unfilteredResultCardsList = unfilteredResultCardsList,
                    useRomajiForKanaWords = mutableStateOf(practicePreferences.readingRomajiFuriganaForKanaWords.get())
                )
            }
        }
    }

}