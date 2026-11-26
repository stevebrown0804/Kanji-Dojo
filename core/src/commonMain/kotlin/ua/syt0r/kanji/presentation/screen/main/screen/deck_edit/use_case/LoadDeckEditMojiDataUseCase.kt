package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.japanese.CharacterClassification
import ua.syt0r.kanji.core.japanese.dakutenHiraganaReadings
import ua.syt0r.kanji.core.japanese.hiraganaReadings
import ua.syt0r.kanji.core.japanese.hiraganaToKatakana
import ua.syt0r.kanji.core.user_data.database.MojiPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration

interface LoadDeckEditMojiDataUseCase {

    suspend operator fun invoke(
        configuration: DeckEditScreenConfiguration.MojiDeck
    ): DeckEditMojiData

}

data class DeckEditMojiData(
    val title: String?,
    val characters: List<String>
)

class DefaultLoadDeckEditMojiDataUseCase(
    private val mojiPracticeRepository: MojiPracticeRepository,
    private val appDataRepository: AppDataRepository
) : LoadDeckEditMojiDataUseCase {

    override suspend operator fun invoke(
        configuration: DeckEditScreenConfiguration.MojiDeck
    ): DeckEditMojiData = withContext(Dispatchers.IO) {
        when (configuration) {

            is DeckEditScreenConfiguration.MojiDeck.CreateNew -> {
                DeckEditMojiData(null, emptyList())
            }

            is DeckEditScreenConfiguration.MojiDeck.CreateDerived -> {
                val characters = when (val classification = configuration.classification) {
                    CharacterClassification.Kana.Hiragana -> {
                        (hiraganaReadings.toList() + dakutenHiraganaReadings.toList()).map {
                            it.first.toString()
                        }
                    }

                    CharacterClassification.Kana.Katakana -> {
                        (hiraganaReadings.toList() + dakutenHiraganaReadings.toList()).map {
                            hiraganaToKatakana(it.first).toString()
                        }
                    }

                    else -> {
                        classification as CharacterClassification.DBDefined
                        appDataRepository.getKanjiForClassification(
                            classification = classification.dbValue
                        )
                    }
                }

                DeckEditMojiData(
                    title = configuration.title,
                    characters = characters
                )
            }

            is DeckEditScreenConfiguration.MojiDeck.Edit -> {
                val deck = mojiPracticeRepository.getDeck(configuration.mojiDeckId)
                val characters =
                    mojiPracticeRepository.getDeckCharacters(configuration.mojiDeckId)
                DeckEditMojiData(
                    title = deck.name,
                    characters = characters
                )
            }

        }

    }

}