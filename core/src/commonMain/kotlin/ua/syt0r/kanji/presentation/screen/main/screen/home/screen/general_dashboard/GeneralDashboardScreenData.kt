package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.srs.MojiPracticeType
import ua.syt0r.kanji.core.srs.PracticeType
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.practice_type_flashcard
import ua.syt0r.kanji.practice_type_reading_picker
import ua.syt0r.kanji.practice_type_writing
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeConfigurationCard
import ua.syt0r.kanji.study_category_moji
import ua.syt0r.kanji.study_category_vocab


sealed interface StudyTargetProgress {

    object NoDecks : StudyTargetProgress

    data class WithDecks(
        val options: StudyTargetPracticeOptions,
        val totalProgress: Float
    ) : StudyTargetProgress

}

data class StudyTargetPracticeOptions(
    val newCards: List<PracticeConfigurationCard>,
    val dueCards: List<PracticeConfigurationCard>,
    val combinedCards: List<PracticeConfigurationCard>
)

data class StudyTargetState(
    val studyTarget: StudyTarget,
    val enabled: Boolean,
    val progress: StudyTargetProgress
)

enum class StudyTarget(
    val categoryTitle: StringResource,
    val typeTitleRes: StringResource,
    val practiceType: PracticeType
) {

    MojiWriting(
        Res.string.study_category_moji,
        Res.string.practice_type_writing,
        MojiPracticeType.Writing
    ),
    MojiFlashcards(
        Res.string.study_category_moji,
        Res.string.practice_type_flashcard,
        MojiPracticeType.Reading
    ),
    VocabFlashcard(
        Res.string.study_category_vocab,
        Res.string.practice_type_flashcard,
        VocabPracticeType.Flashcard
    ),
    VocabWriting(
        Res.string.study_category_vocab,
        Res.string.practice_type_writing,
        VocabPracticeType.Writing
    ),
    VocabReadingPicker(
        Res.string.study_category_vocab,
        Res.string.practice_type_reading_picker,
        VocabPracticeType.ReadingPicker
    );

    val preferenceKey: String
        get() = when (this) {
            MojiWriting -> "LetterWriting"
            MojiFlashcards -> "LetterFlashcards"
            else -> name
        }

}

data class StreakCalendarItem(
    val date: LocalDate,
    val anyReviews: Boolean
)

data class GeneralDashboardStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val reviewsToday: Int
)
