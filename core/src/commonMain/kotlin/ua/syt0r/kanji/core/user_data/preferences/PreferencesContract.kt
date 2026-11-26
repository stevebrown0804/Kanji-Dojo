package ua.syt0r.kanji.core.user_data.preferences

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty


interface PreferencesContract {

    interface AppPreferences {


        val localDataId: SuspendedProperty<String>
        val localDataTimestamp: SuspendedProperty<Instant?>


        val practiceType: SuspendedProperty<PreferencesMojiPracticeType>
        val filterNew: SuspendedProperty<Boolean>
        val filterDue: SuspendedProperty<Boolean>
        val filterDone: SuspendedProperty<Boolean>
        val sortOption: SuspendedProperty<PreferencesMojiSortOption>

        val isSortDescending: SuspendedProperty<Boolean>

        val practicePreviewLayout: SuspendedProperty<PreferencesDeckDetailsMojiLayout>

        val kanaGroupsEnabled: SuspendedProperty<Boolean>

        val theme: SuspendedProperty<PreferencesTheme>

        val dailyLimitEnabled: SuspendedProperty<Boolean>
        val dailyLimitConfigurationJson: SuspendedProperty<String>

        val reminderEnabled: SuspendedProperty<Boolean>
        val reminderTime: SuspendedProperty<LocalTime>

        val defaultHomeTab: SuspendedProperty<PreferencesDefaultHomeTab>

        val lastAppVersionWhenChangesDialogShown: SuspendedProperty<String>
        val tutorialSeen: SuspendedProperty<Boolean>
        val generalDashboardStudyTargets: SuspendedProperty<Map<String, Boolean>>

        val mojiDashboardPracticeType: SuspendedProperty<PreferencesMojiPracticeType>
        val mojiDashboardSortByTime: SuspendedProperty<Boolean>

        val vocabDashboardPracticeType: SuspendedProperty<PreferencesVocabPracticeType>
        val vocabDashboardSortByTime: SuspendedProperty<Boolean>

        val dailyResetTime: SuspendedProperty<LocalTime>
    }

    interface PracticePreferences {

        val shuffle: SuspendedProperty<Boolean>
        val newCardsOrder: SuspendedProperty<PreferencesNewCardsOrder>

        val noTranslationLayout: SuspendedProperty<Boolean>
        val leftHandMode: SuspendedProperty<Boolean>
        val altStrokeEvaluator: SuspendedProperty<Boolean>

        val highlightRadicals: SuspendedProperty<Boolean>
        val kanaAutoPlay: SuspendedProperty<Boolean>

        val writingInputMethod: SuspendedProperty<PreferencesMojiPracticeWritingInputMode>
        val writingRomajiInsteadOfKanaWords: SuspendedProperty<Boolean>

        val readingRomajiFuriganaForKanaWords: SuspendedProperty<Boolean>

        val vocabFlashcardMeaningInFront: SuspendedProperty<Boolean>
        val vocabReadingPickerShowMeaning: SuspendedProperty<Boolean>
        val vocabWritingShowKanaReading: SuspendedProperty<Boolean>

    }

}

enum class PreferencesNewCardsOrder { First, Last, Mixed }
enum class PreferencesMojiPracticeType { Writing, Reading }
enum class PreferencesMojiSortOption { AddOrder, Frequency, Name, ReviewTime }
enum class PreferencesDeckDetailsMojiLayout { Character, Groups }
enum class PreferencesTheme { System, Light, Dark, Amoled }
enum class PreferencesMojiPracticeWritingInputMode { Stroke, Character }
enum class PreferencesVocabPracticeType { Flashcard, ReadingPicker, Writing }
enum class PreferencesDefaultHomeTab {
    GeneralDashboard, Moji, Vocab;

    val storageName: String
        get() = when (this) {
            Moji -> "Letters"
            else -> name
        }
}
