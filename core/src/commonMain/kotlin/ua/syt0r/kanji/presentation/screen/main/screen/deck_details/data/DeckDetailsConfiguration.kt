package ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import ua.syt0r.kanji.core.user_data.preferences.PreferencesDeckDetailsMojiLayout
import ua.syt0r.kanji.core.user_data.preferences.PreferencesMojiSortOption
import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.common.ScreenVocabPracticeType
import ua.syt0r.kanji.presentation.common.resources.string.StringResolveScope


sealed interface DeckDetailsConfiguration {

    data class MojiDeckConfiguration(
        val practiceType: ScreenMojiPracticeType,
        val filterConfiguration: FilterConfiguration,
        val sortOption: MojiSortOption,
        val isDescending: Boolean,
        val layout: DeckDetailsLayout,
        val kanaGroups: Boolean,
    ) : DeckDetailsConfiguration

    data class VocabDeckConfiguration(
        val practiceType: ScreenVocabPracticeType,
        val filterConfiguration: FilterConfiguration
    ) : DeckDetailsConfiguration

}

data class FilterConfiguration(
    val showNew: Boolean,
    val showDue: Boolean,
    val showDone: Boolean,
)

enum class MojiSortOption(
    val titleResolver: StringResolveScope<String>,
    val hintResolver: StringResolveScope<String>,
    val preferencesType: PreferencesMojiSortOption,
) {
    ADD_ORDER(
        titleResolver = { deckDetails.sortDialog.sortOptionAddOrder },
        hintResolver = { deckDetails.sortDialog.sortOptionAddOrderHint },
        preferencesType = PreferencesMojiSortOption.AddOrder
    ),
    FREQUENCY(
        titleResolver = { deckDetails.sortDialog.sortOptionFrequency },
        hintResolver = { deckDetails.sortDialog.sortOptionFrequencyHint },
        preferencesType = PreferencesMojiSortOption.Frequency
    ),
    NAME(
        titleResolver = { deckDetails.sortDialog.sortOptionName },
        hintResolver = { deckDetails.sortDialog.sortOptionNameHint },
        preferencesType = PreferencesMojiSortOption.Name
    ),
    REVIEW_TIME(
        titleResolver = { deckDetails.sortDialog.sortOptionReviewTime },
        hintResolver = { deckDetails.sortDialog.sortOptionReviewTimeHint },
        preferencesType = PreferencesMojiSortOption.ReviewTime
    );

    val imageVector = Icons.AutoMirrored.Filled.ArrowForward
}

fun PreferencesMojiSortOption.toScreenType() =
    MojiSortOption.values().find { it.preferencesType == this }!!


enum class DeckDetailsLayout(
    val titleResolver: StringResolveScope<String>,
    val correspondingRepoType: PreferencesDeckDetailsMojiLayout,
) {
    SingleCharacter(
        titleResolver = { deckDetails.layoutDialog.singleCharacterOptionLabel },
        correspondingRepoType = PreferencesDeckDetailsMojiLayout.Character
    ),
    Groups(
        titleResolver = { deckDetails.layoutDialog.groupsOptionLabel },
        correspondingRepoType = PreferencesDeckDetailsMojiLayout.Groups
    )
}

fun PreferencesDeckDetailsMojiLayout.toScreenType() = DeckDetailsLayout.values()
    .find { it.correspondingRepoType == this }!!
