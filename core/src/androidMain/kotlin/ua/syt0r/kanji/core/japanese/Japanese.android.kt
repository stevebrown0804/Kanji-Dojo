package ua.syt0r.kanji.core.japanese

import dev.esnault.wanakana.core.Wanakana
import java.lang.Character.UnicodeBlock
import java.text.Normalizer

actual fun String.kanaToRomaji(): String = Wanakana.toRomaji(this)
actual fun String.readingInputToHiragana(): String =
    Wanakana.toHiragana(Normalizer.normalize(this, Normalizer.Form.NFKC))

actual fun Char.isKanji(): Boolean = UnicodeBlock.of(this) == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
actual fun Char.isHiragana(): Boolean = UnicodeBlock.of(this) == UnicodeBlock.HIRAGANA
actual fun Char.isKatakana(): Boolean = UnicodeBlock.of(this) == UnicodeBlock.KATAKANA
actual fun Char.isKana(): Boolean = isHiragana() || isKatakana()
