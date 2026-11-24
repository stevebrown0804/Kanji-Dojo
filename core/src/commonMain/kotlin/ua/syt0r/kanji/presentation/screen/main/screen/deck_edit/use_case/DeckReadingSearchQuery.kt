package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case

import ua.syt0r.kanji.core.japanese.katakanaToHiragana
import ua.syt0r.kanji.core.japanese.readingInputToHiragana

internal class DeckReadingSearchQuery private constructor(
    private val input: String,
    private val readings: Set<String>
) {

    val hasReadings: Boolean get() = readings.isNotEmpty()

    fun matches(character: String, readings: Collection<String>): Boolean {
        if (input.isBlank()) return true
        if (character.any { !it.isReadingCharacter() } && character in input) {
            return true
        }

        return hasReadings && readings.any { reading ->
            val normalized = reading.trim('-').normalizeKana()
            normalized.replace(".", "") in this.readings ||
                normalized.substringBefore('.') in this.readings
        }
    }

    companion object {

        private val hiraganaPattern = Regex("[ぁ-ゖ\u3099\u309Aゝ-ゟー]+")

        fun parse(input: String): DeckReadingSearchQuery {
            val tokenInput = buildString(input.length) {
                for (character in input) {
                    append(if (character.isReadingCharacter()) character else ' ')
                }
            }
            val readings = tokenInput.splitToSequence(' ')
                .filter(String::isNotEmpty)
                .map { it.lowercase().normalizeKana().readingInputToHiragana().normalizeKana() }
                .filter(hiraganaPattern::matches)
                .toSet()

            return DeckReadingSearchQuery(input, readings)
        }

        private fun Char.isReadingCharacter(): Boolean = when (this) {
            in 'ぁ'..'ゖ', '\u3099', '\u309A', in 'ゝ'..'ゟ',
            in 'ァ'..'ヺ', in 'ー'..'ヿ', in 'ｦ'..'ﾟ', in '\u0300'..'\u036F' -> true
            in '\u3400'..'\u4DBF', in '\u4E00'..'\u9FFF', in '\uF900'..'\uFAFF',
            '々', '〆', '〇', '〻' -> false
            else -> isLetter()
        }

        private fun String.normalizeKana(): String = buildString(length) {
            for (character in this@normalizeKana) {
                append(
                    when (character) {
                        in 'ァ'..'ヶ', 'ヽ', 'ヾ' -> katakanaToHiragana(character)
                        else -> character
                    }
                )
            }
        }

    }

}
