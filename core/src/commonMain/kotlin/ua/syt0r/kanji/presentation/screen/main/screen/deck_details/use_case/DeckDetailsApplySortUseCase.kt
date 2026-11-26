package ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case

import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsItemData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.MojiSortOption

interface DeckDetailsApplySortUseCase {
    operator fun invoke(
        items: List<DeckDetailsItemData.MojiData>,
        practiceType: ScreenMojiPracticeType,
        sortOption: MojiSortOption,
        isDescending: Boolean,
    ): List<DeckDetailsItemData.MojiData>
}

class DefaultDeckDetailsApplySortUseCase : DeckDetailsApplySortUseCase {

    override fun invoke(
        items: List<DeckDetailsItemData.MojiData>,
        practiceType: ScreenMojiPracticeType,
        sortOption: MojiSortOption,
        isDescending: Boolean,
    ): List<DeckDetailsItemData.MojiData> {
        val comparator = when (sortOption) {
            MojiSortOption.ADD_ORDER -> {

                val reviewDateComparator: Comparator<DeckDetailsItemData.MojiData> = when (
                    isDescending
                ) {
                    true -> compareByDescending { it.positionInPractice }
                    false -> compareBy { it.positionInPractice }
                }

                reviewDateComparator

            }

            MojiSortOption.NAME -> {

                val nameComparator: Comparator<DeckDetailsItemData.MojiData> =
                    when (isDescending) {
                        true -> compareByDescending { it.character }
                        false -> compareBy { it.character }
                    }

                nameComparator

            }

            MojiSortOption.FREQUENCY -> {

                val frequencyComparator: Comparator<DeckDetailsItemData.MojiData> =
                    when (isDescending) {
                        true -> compareByDescending { it.frequency }
                        false -> compareBy { it.frequency }
                    }

                frequencyComparator.thenBy { it.character }

            }

            MojiSortOption.REVIEW_TIME -> {
                when {
                    isDescending -> compareByDescending {
                        it.summaryMap.getValue(practiceType).expectedReviewDate
                    }

                    else -> compareBy {
                        it.summaryMap.getValue(practiceType).expectedReviewDate
                    }
                }
            }
        }

        return items.sortedWith(comparator)
    }

}
