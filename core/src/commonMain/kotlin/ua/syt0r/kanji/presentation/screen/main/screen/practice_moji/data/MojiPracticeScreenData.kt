package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.core.app_data.data.CharacterRadical
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.japanese.CharacterClassification
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.core.user_data.preferences.PreferencesMojiPracticeWritingInputMode
import ua.syt0r.kanji.presentation.common.Paginateable
import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.common.resources.string.StringResolveScope
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriterState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.DisplayableEnum
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswers
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeConfigurationCard
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeConfigurationCardsSelectorState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.MojiPracticeCardConfigurationData

@Serializable
@SerialName("ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration")
data class MojiPracticeScreenConfiguration(
    val practiceType: ScreenMojiPracticeType,
    val cards: List<Card>,
) {

    @Serializable
    @SerialName("ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration.Card")
    data class Card(
        @SerialName("letter")
        val moji: String,
        val deckId: Long
    ) : PracticeConfigurationCard

}

sealed interface MojiPracticeConfiguration {

    val practiceType: ScreenMojiPracticeType
    val selectorState: PracticeConfigurationCardsSelectorState
    val unfilteredResultCardsList: State<List<MojiPracticeCardConfigurationData>>

    data class Writing(
        override val selectorState: PracticeConfigurationCardsSelectorState,
        override val unfilteredResultCardsList: State<List<MojiPracticeCardConfigurationData>>,
        val hintMode: MutableState<WritingPracticeHintMode>,
        val inputMode: MutableState<WritingPracticeInputMode>,
        val useRomajiForKanaWords: MutableState<Boolean>,
        val noTranslationsLayout: MutableState<Boolean>,
        val leftHandedMode: MutableState<Boolean>,
        val altStrokeEvaluatorEnabled: MutableState<Boolean>,
    ) : MojiPracticeConfiguration {
        override val practiceType: ScreenMojiPracticeType = ScreenMojiPracticeType.Writing
    }

    data class Reading(
        override val selectorState: PracticeConfigurationCardsSelectorState,
        override val unfilteredResultCardsList: State<List<MojiPracticeCardConfigurationData>>,
        val useRomajiForKanaWords: MutableState<Boolean>
    ) : MojiPracticeConfiguration {
        override val practiceType: ScreenMojiPracticeType = ScreenMojiPracticeType.Reading
    }

}

sealed interface MojiPracticeLayoutConfiguration {

    val kanaAutoPlay: MutableState<Boolean>

    data class WritingLayoutConfiguration(
        val noTranslationsLayout: Boolean,
        val radicalsHighlight: MutableState<Boolean>,
        override val kanaAutoPlay: MutableState<Boolean>,
        val leftHandedMode: Boolean
    ) : MojiPracticeLayoutConfiguration

    data class ReadingLayoutConfiguration(
        override val kanaAutoPlay: MutableState<Boolean>
    ) : MojiPracticeLayoutConfiguration

}


sealed interface MojiPracticeReviewState {

    val layout: MojiPracticeLayoutConfiguration
    val itemData: MojiPracticeItemData

    data class Writing(
        override val layout: MojiPracticeLayoutConfiguration.WritingLayoutConfiguration,
        override val itemData: MojiPracticeItemData.WritingData,
        val answers: PracticeAnswers,
        val studyWriterState: CharacterWriterState?,
        val reviewWriterState: CharacterWriterState
    ) : MojiPracticeReviewState {
        val isStudyMode = mutableStateOf(value = studyWriterState != null)
        val writerState: State<CharacterWriterState> = derivedStateOf {
            if (isStudyMode.value) studyWriterState!! else reviewWriterState
        }
    }

    data class Reading(
        override val layout: MojiPracticeLayoutConfiguration.ReadingLayoutConfiguration,
        override val itemData: MojiPracticeItemData.ReadingData,
        val answers: PracticeAnswers,
        val revealed: MutableState<Boolean> = mutableStateOf(false)
    ) : MojiPracticeReviewState

}

enum class WritingPracticeHintMode(
    override val titleResolver: StringResolveScope<String>,
) : DisplayableEnum {
    OnlyNew(
        titleResolver = { mojiPractice.hintStrokeNewOnlyMode }
    ),
    All(
        titleResolver = { mojiPractice.hintStrokeAllMode }
    ),
    None(
        titleResolver = { mojiPractice.hintStrokeNoneMode }
    )
}

enum class WritingPracticeInputMode(
    override val titleResolver: StringResolveScope<String>,
    val repoType: PreferencesMojiPracticeWritingInputMode
) : DisplayableEnum {
    Stroke(
        titleResolver = { mojiPractice.inputModeStroke },
        repoType = PreferencesMojiPracticeWritingInputMode.Stroke
    ),
    Character(
        titleResolver = { mojiPractice.inputModeCharacter },
        repoType = PreferencesMojiPracticeWritingInputMode.Character
    )
}

fun PreferencesMojiPracticeWritingInputMode.toScreenType(): WritingPracticeInputMode =
    WritingPracticeInputMode.entries.first { it.repoType == this }

interface MojiPracticeItemData {

    interface KanaData : MojiPracticeItemData {
        val kanaSystem: CharacterClassification.Kana
        val reading: KanaReading
    }

    interface KanjiData : MojiPracticeItemData {
        val radicals: List<CharacterRadical>
        val on: List<String>
        val kun: List<String>
        val meanings: List<String>
        val variants: String?
    }

    sealed interface WritingData : MojiPracticeItemData {
        val strokes: List<Path>
    }

    sealed interface ReadingData : MojiPracticeItemData

    val character: String
    val examples: Paginateable<MojiPracticeExampleWord>

    data class KanaWritingData(
        override val character: String,
        override val strokes: List<Path>,
        override val examples: Paginateable<MojiPracticeExampleWord>,
        override val kanaSystem: CharacterClassification.Kana,
        override val reading: KanaReading
    ) : WritingData, KanaData

    data class KanaReadingData(
        override val character: String,
        override val examples: Paginateable<MojiPracticeExampleWord>,
        override val kanaSystem: CharacterClassification.Kana,
        override val reading: KanaReading
    ) : ReadingData, KanaData

    data class KanjiWritingData(
        override val character: String,
        override val strokes: List<Path>,
        override val examples: Paginateable<MojiPracticeExampleWord>,
        override val radicals: List<CharacterRadical>,
        override val on: List<String>,
        override val kun: List<String>,
        override val meanings: List<String>,
        override val variants: String?
    ) : WritingData, KanjiData

    data class KanjiReadingData(
        override val character: String,
        override val examples: Paginateable<MojiPracticeExampleWord>,
        override val radicals: List<CharacterRadical>,
        override val on: List<String>,
        override val kun: List<String>,
        override val meanings: List<String>,
        override val variants: String?
    ) : ReadingData, KanjiData

}

data class MojiPracticeExampleWord(
    val word: JapaneseWord,
    val romaji: String?
)
