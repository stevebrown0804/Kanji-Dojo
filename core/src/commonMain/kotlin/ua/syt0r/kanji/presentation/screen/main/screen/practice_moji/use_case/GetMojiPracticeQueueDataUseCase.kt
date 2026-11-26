package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ua.syt0r.kanji.core.srs.MojiPracticeType
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.stroke_evaluator.AltKanjiStrokeEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.DefaultKanjiStrokeEvaluator
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeLayoutConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueItemDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.WritingPracticeHintMode

interface GetMojiPracticeQueueDataUseCase {

    suspend operator fun invoke(
        configuration: MojiPracticeConfiguration
    ): List<MojiPracticeQueueItemDescriptor>

}

class DefaultGetMojiPracticeQueueDataUseCase(
    private val practicePreferences: PreferencesContract.PracticePreferences,
    private val srsCardRepository: SrsCardRepository,
    private val configurationUpdateScope: CoroutineScope
) : GetMojiPracticeQueueDataUseCase {

    override suspend fun invoke(
        configuration: MojiPracticeConfiguration
    ): List<MojiPracticeQueueItemDescriptor> {

        val kanaAutoPlay = mutableStateOf(
            value = practicePreferences.kanaAutoPlay.get()
        )

        snapshotFlow { kanaAutoPlay.value }
            .onEach { practicePreferences.kanaAutoPlay.set(it) }
            .launchIn(configurationUpdateScope)

        return when (configuration) {
            is MojiPracticeConfiguration.Writing -> {

                val radicalsHighlight = mutableStateOf(
                    value = practicePreferences.highlightRadicals.get()
                )

                snapshotFlow { radicalsHighlight.value }
                    .onEach { practicePreferences.highlightRadicals.set(it) }
                    .launchIn(configurationUpdateScope)

                val layout = MojiPracticeLayoutConfiguration.WritingLayoutConfiguration(
                    noTranslationsLayout = configuration.noTranslationsLayout.value,
                    radicalsHighlight = radicalsHighlight,
                    kanaAutoPlay = kanaAutoPlay,
                    leftHandedMode = configuration.leftHandedMode.value
                )

                val evaluator = when (configuration.altStrokeEvaluatorEnabled.value) {
                    true -> AltKanjiStrokeEvaluator()
                    false -> DefaultKanjiStrokeEvaluator()
                }

                configuration.unfilteredResultCardsList.value
                    .take(configuration.selectorState.selectedCountIntState.value)
                    .map { (character, deckId) ->
                        val shouldStudy: Boolean = when (configuration.hintMode.value) {
                            WritingPracticeHintMode.OnlyNew -> {
                                val isNew = srsCardRepository
                                    .get(MojiPracticeType.Writing.toSrsKey(character)) == null
                                isNew
                            }

                            WritingPracticeHintMode.All -> true
                            WritingPracticeHintMode.None -> false
                        }
                        MojiPracticeQueueItemDescriptor.Writing(
                            character = character,
                            deckId = deckId,
                            romajiReading = configuration.useRomajiForKanaWords.value,
                            layoutConfiguration = layout,
                            inputMode = configuration.inputMode.value,
                            evaluator = evaluator,
                            shouldStudy = shouldStudy
                        )
                    }
            }

            is MojiPracticeConfiguration.Reading -> {

                val layout = MojiPracticeLayoutConfiguration.ReadingLayoutConfiguration(
                    kanaAutoPlay = kanaAutoPlay
                )

                configuration.unfilteredResultCardsList.value
                    .take(configuration.selectorState.selectedCountIntState.value)
                    .map { (character, deckId) ->
                        MojiPracticeQueueItemDescriptor.Reading(
                            character = character,
                            deckId = deckId,
                            romajiReading = configuration.useRomajiForKanaWords.value,
                            layoutConfiguration = layout
                        )
                    }

            }
        }
    }

}