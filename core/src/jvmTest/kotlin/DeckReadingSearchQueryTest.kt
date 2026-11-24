import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DeckReadingSearchQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeckReadingSearchQueryTest {

    private val deck = linkedMapOf(
        "一" to listOf("イチ", "イツ", "ひと-", "ひと.つ"),
        "人" to listOf("ジン", "ニン", "ひと"),
        "国" to listOf("コク", "くに"),
        "休" to listOf("キュウ", "やす.む"),
        "日" to listOf("ニチ", "ひ", "-び"),
        "𠀀" to emptyList(),
        "ひ" to emptyList()
    )

    @Test
    fun matchesAnyReadingInDeckOrderAcrossScripts() {
        assertEquals(listOf("一", "人", "国"), search("クニ／HITO🚀123、いち"))
        assertEquals(listOf("一", "人"), search("ヒト, イチ"))
        assertEquals(listOf("国"), search("ｺｸ"))
    }

    @Test
    fun matchesLiteralKanjiAndOnlyCompleteReadingsOrTheirStems() {
        assertEquals(listOf("一", "人", "𠀀"), search("𠀀hito"))
        assertEquals(listOf("国", "休"), search("国・yasumu"))
        assertEquals(listOf("休"), search("やす"))
        assertEquals(listOf("日"), search("bi"))
        assertEquals(listOf("日"), search("hi"))
        assertEquals(emptyList(), search("yas"))
    }

    @Test
    fun clearsTheFilterOnlyForBlankInput() {
        assertEquals(deck.keys.toList(), search(" \t"))
        assertEquals(emptyList(), search("!?42"))
        assertFalse(DeckReadingSearchQuery.parse("国").hasReadings)
        assertTrue(DeckReadingSearchQuery.parse("いち").hasReadings)
    }

    @Test
    fun keepsOtherAlphabeticCharactersInsideTheirReadingToken() {
        assertEquals(emptyList(), search("hitoéichi"))
        assertEquals(emptyList(), search("hito\u0304ichi"))
    }

    private fun search(input: String): List<String> {
        val query = DeckReadingSearchQuery.parse(input)
        return deck.filter { (character, readings) -> query.matches(character, readings) }.keys.toList()
    }

}
