package ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case

import ua.syt0r.kanji.core.japanese.hiraganaToKatakana
import ua.syt0r.kanji.core.srs.SrsItemStatus
import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsItemData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsListItem
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsListItemKey
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.PracticeGroupSummary

interface DeckDetailsCreateMojiGroupsUseCase {

    operator fun invoke(
        items: List<DeckDetailsItemData.MojiData>,
        visibleItems: List<DeckDetailsItemData>,
        type: ScreenMojiPracticeType,
        probeKanaGroups: Boolean,
        previousSelectionStates: Map<DeckDetailsListItemKey, Boolean>?,
    ): MojiGroupsCreationResult

}

data class MojiGroupsCreationResult(
    val kanaGroups: Boolean,
    val groups: List<DeckDetailsListItem.Group>
)

class DefaultDeckDetailsCreateMojiGroupsUseCase :
    DeckDetailsCreateMojiGroupsUseCase {

    companion object {
        private const val CharactersInGroup = 6

        private val HiraganaBaseGroups = listOf(
            "あいうえお",
            "かきくけこ",
            "さしすせそ",
            "たちつてと",
            "なにぬねの",
            "はひふへほ",
            "まみむめも",
            "らりるれろ",
            "やゆよ",
            "わをん"
        )

        private val HiraganaFullGroups = HiraganaBaseGroups + listOf(
            "がぎぐげご",
            "ざじずぜぞ",
            "だぢづでど",
            "ばびぶべぼ",
            "ぱぴぷぺぽ"
        )

        private val KatakanaBaseGroups = HiraganaBaseGroups.groupsToKatakana()
        private val KatakanaFullGroups = HiraganaFullGroups.groupsToKatakana()

        private fun List<String>.groupsToKatakana(): List<String> = map { groupString ->
            groupString.toCharArray().joinToString("") { hiraganaToKatakana(it).toString() }
        }

    }


    override fun invoke(
        items: List<DeckDetailsItemData.MojiData>,
        visibleItems: List<DeckDetailsItemData>,
        type: ScreenMojiPracticeType,
        probeKanaGroups: Boolean,
        previousSelectionStates: Map<DeckDetailsListItemKey, Boolean>?,
    ): MojiGroupsCreationResult {

        val itemsMap = items.associateBy { it.character }
        val characters = itemsMap.keys

        val (chunkedGroups: List<List<DeckDetailsItemData>>, kanaGroupsFound: Boolean) = when {
            !probeKanaGroups -> visibleItems.chunked(CharactersInGroup) to false

            HiraganaBaseGroups.areMatchingSets(characters) -> {
                HiraganaBaseGroups.associateGroupsWithItems(itemsMap) to true
            }

            HiraganaFullGroups.areMatchingSets(characters) -> {
                HiraganaFullGroups.associateGroupsWithItems(itemsMap) to true
            }

            KatakanaBaseGroups.areMatchingSets(characters) -> {
                KatakanaBaseGroups.associateGroupsWithItems(itemsMap) to true
            }

            KatakanaFullGroups.areMatchingSets(characters) -> {
                KatakanaFullGroups.associateGroupsWithItems(itemsMap) to true
            }

            else -> visibleItems.chunked(CharactersInGroup) to false
        }

        val groups = chunkedGroups.mapIndexed { index, groupItems ->
            createGroup(
                index = index,
                groupItems = groupItems as List<DeckDetailsItemData.MojiData>,
                practiceType = type,
                previousSelectionStates = previousSelectionStates,
            )
        }

        return MojiGroupsCreationResult(
            kanaGroups = kanaGroupsFound,
            groups = groups
        )
    }

    private fun List<String>.areMatchingSets(characters: Set<String>): Boolean {
        val set = flatMap { it.map { it.toString() } }.toSet()
        return set.size == characters.size && characters.intersect(set).size == set.size
    }

    private fun List<String>.associateGroupsWithItems(
        itemsMap: Map<String, DeckDetailsItemData>,
    ): List<List<DeckDetailsItemData>> = map { groupString ->
        groupString.map { char -> itemsMap.getValue(char.toString()) }
    }

    private fun createGroup(
        index: Int,
        groupItems: List<DeckDetailsItemData.MojiData>,
        practiceType: ScreenMojiPracticeType,
        previousSelectionStates: Map<DeckDetailsListItemKey, Boolean>?,
    ): DeckDetailsListItem.Group {

        val itemReviewStates = groupItems.map { it.summaryMap.getValue(practiceType).srsItemStatus }

        val groupReviewState = when {
            itemReviewStates.all { it == SrsItemStatus.Done } -> SrsItemStatus.Done
            itemReviewStates.any { it == SrsItemStatus.Review } -> SrsItemStatus.Review
            else -> SrsItemStatus.New
        }

        val summary = when (practiceType) {
            ScreenMojiPracticeType.Writing -> PracticeGroupSummary(
                firstReviewDate = groupItems
                    .mapNotNull {
                        it.summaryMap.getValue(ScreenMojiPracticeType.Writing).firstReviewDate
                    }
                    .minOrNull(),
                lastReviewDate = groupItems
                    .mapNotNull {
                        it.summaryMap.getValue(ScreenMojiPracticeType.Writing).lastReviewDate
                    }
                    .maxOrNull(),
                state = groupReviewState
            )

            ScreenMojiPracticeType.Reading -> PracticeGroupSummary(
                firstReviewDate = groupItems
                    .mapNotNull {
                        it.summaryMap.getValue(ScreenMojiPracticeType.Reading).firstReviewDate
                    }
                    .minOrNull(),
                lastReviewDate = groupItems
                    .mapNotNull {
                        it.summaryMap.getValue(ScreenMojiPracticeType.Reading).lastReviewDate
                    }
                    .maxOrNull(),
                state = groupReviewState
            )
        }

        val key = DeckDetailsListItemKey(
            value = groupItems.joinToString("") { it.character }
        )

        return DeckDetailsListItem.Group(
            index = index + 1,
            key = key,
            items = groupItems,
            summary = summary,
            reviewState = groupReviewState,
            initialSelectionState = previousSelectionStates?.get(key) ?: false
        )
    }

}
