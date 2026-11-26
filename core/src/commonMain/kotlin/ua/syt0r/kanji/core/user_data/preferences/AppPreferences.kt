package ua.syt0r.kanji.core.user_data.preferences

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import ua.syt0r.kanji.core.suspended_property.BooleanSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.EnumSuspendedPropertyType.Companion.enumSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.InstantSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.LocalTimeSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.StringSuspendedPropertyType
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import ua.syt0r.kanji.core.suspended_property.SuspendedPropertyCreatorScope
import ua.syt0r.kanji.core.suspended_property.jsonPojoSuspendedPropertyType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AppPreferences(
    creatorScope: SuspendedPropertyCreatorScope
) : PreferencesContract.AppPreferences,
    SuspendedPropertyCreatorScope by creatorScope {

    @OptIn(ExperimentalUuidApi::class)
    override val localDataId: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "local_data_id",
        initialValue = { Uuid.random().toHexDashString() },
        saveInitialValue = true
    )

    override val localDataTimestamp: SuspendedProperty<Instant?> = createNullableProperty(
        type = InstantSuspendedPropertyType,
        key = "local_data_timestamp",
        initialValue = { null }
    )

    override val practiceType: SuspendedProperty<PreferencesMojiPracticeType> = createProperty(
        type = enumSuspendedPropertyType<PreferencesMojiPracticeType>(),
        key = "practice_type",
        initialValue = { PreferencesMojiPracticeType.Writing }
    )

    override val filterNew: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "filter_new",
        initialValue = { true }
    )

    override val filterDue: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "filter_due",
        initialValue = { true }
    )

    override val filterDone: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "filter_done",
        initialValue = { true }
    )

    override val sortOption: SuspendedProperty<PreferencesMojiSortOption> = createProperty(
        type = enumSuspendedPropertyType<PreferencesMojiSortOption>(),
        key = "sort_option",
        initialValue = { PreferencesMojiSortOption.AddOrder }
    )

    override val isSortDescending: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "is_desc",
        initialValue = { false }
    )

    override val practicePreviewLayout: SuspendedProperty<PreferencesDeckDetailsMojiLayout> =
        createProperty(
            type = enumSuspendedPropertyType<PreferencesDeckDetailsMojiLayout>(),
            key = "practice_preview_layout2",
            initialValue = { PreferencesDeckDetailsMojiLayout.Groups }
        )

    override val kanaGroupsEnabled: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "kana_groups_enabled",
        initialValue = { true }
    )

    override val theme: SuspendedProperty<PreferencesTheme> = createProperty(
        type = enumSuspendedPropertyType<PreferencesTheme>(),
        key = "theme",
        initialValue = { PreferencesTheme.System }
    )

    override val dailyLimitEnabled: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "daily_limit_enabled",
        initialValue = { true },
        affectSync = true
    )

    override val dailyLimitConfigurationJson: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "daily_limit_configuration",
        initialValue = { "" },
        affectSync = true
    )

    override val reminderEnabled: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "reminder_enabled",
        initialValue = { false },
        enableBackup = false
    )

    override val reminderTime: SuspendedProperty<LocalTime> = createProperty(
        type = LocalTimeSuspendedPropertyType,
        key = "reminder_time",
        initialValue = { LocalTime(hour = 9, minute = 0) },
        affectSync = true
    )

    override val defaultHomeTab: SuspendedProperty<PreferencesDefaultHomeTab> = createProperty(
        type = enumSuspendedPropertyType { it.storageName },
        key = "default_home_tab",
        affectSync = true,
        initialValue = { PreferencesDefaultHomeTab.GeneralDashboard }
    )

    override val lastAppVersionWhenChangesDialogShown: SuspendedProperty<String> = createProperty(
        type = StringSuspendedPropertyType,
        key = "last_changes_dialog_version_shown",
        initialValue = { "" }
    )

    override val tutorialSeen: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "tutorial_seen",
        initialValue = { false }
    )

    override val generalDashboardStudyTargets: SuspendedProperty<Map<String, Boolean>> =
        createProperty(
            type = jsonPojoSuspendedPropertyType(),
            key = "general_dashboard_study_targets",
            initialValue = { emptyMap() },
            affectSync = true
        )

    override val mojiDashboardPracticeType: SuspendedProperty<PreferencesMojiPracticeType> =
        createProperty(
            type = enumSuspendedPropertyType<PreferencesMojiPracticeType>(),
            key = "letter_dashboard_practice_type",
            initialValue = { PreferencesMojiPracticeType.Writing }
        )

    override val mojiDashboardSortByTime: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "letter_dashboard_sort_by_time",
        initialValue = { false }
    )

    override val vocabDashboardPracticeType: SuspendedProperty<PreferencesVocabPracticeType> =
        createProperty(
            type = enumSuspendedPropertyType<PreferencesVocabPracticeType>(),
            key = "vocab_dashboard_practice_type",
            initialValue = { PreferencesVocabPracticeType.Flashcard }
        )

    override val vocabDashboardSortByTime: SuspendedProperty<Boolean> = createProperty(
        type = BooleanSuspendedPropertyType,
        key = "vocab_dashboard_sort_by_time",
        initialValue = { false }
    )

    override val dailyResetTime: SuspendedProperty<LocalTime> = createProperty(
        type = LocalTimeSuspendedPropertyType,
        key = "daily_reset_time",
        initialValue = { LocalTime(0, 0) },
        affectSync = true
    )

}
