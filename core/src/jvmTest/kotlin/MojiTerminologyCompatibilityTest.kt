import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import ua.syt0r.kanji.core.srs.DefaultDailyLimitManager
import ua.syt0r.kanji.core.srs.MojiPracticeType
import ua.syt0r.kanji.core.srs.PracticeLimit
import ua.syt0r.kanji.core.time.DefaultTimeUtils
import ua.syt0r.kanji.core.user_data.preferences.DataStorePreferencesManager
import ua.syt0r.kanji.core.user_data.preferences.DefaultPreferencesBackupManager
import ua.syt0r.kanji.core.user_data.preferences.DefaultUserPreferencesMigrationManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesDefaultHomeTab
import ua.syt0r.kanji.core.user_data.preferences.PreferencesMojiPracticeType
import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.configuration
import ua.syt0r.kanji.presentation.screen.main.defaultMainDestinations
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.data.DeckPickerScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeScreenTab
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.StudyTarget
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MojiTerminologyCompatibilityTest {

    @Test
    fun importsAndExportsLegacyPreferenceBackupWithCustomMojiSettings() = runBlocking {
        val dataStore = InMemoryPreferencesDataStore()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val manager = DataStorePreferencesManager(
            dataStore = dataStore,
            timeUtils = DefaultTimeUtils,
            migrationManager = DefaultUserPreferencesMigrationManager(dataStore),
            coroutineScope = scope
        )
        val backup = DefaultPreferencesBackupManager(manager, manager, DefaultTimeUtils)
        val legacyLimits = """
            {
                "isLetterLimitCombined": false,
                "letterCombinedLimit": {"new": 7, "due": 71},
                "letterSeparatedLimit": {
                    "Writing": {"new": 2, "due": 22},
                    "Reading": {"new": 3, "due": 33}
                }
            }
        """.trimIndent()
        val legacyTargets = """{"LetterWriting":false,"LetterFlashcards":true,"VocabWriting":false}"""
        val legacyBackup = buildJsonObject {
            put("local_data_id", "legacy-user-data")
            put("local_data_timestamp", 1000)
            put("default_home_tab", "Letters")
            put("general_dashboard_study_targets", legacyTargets)
            put("daily_limit_configuration", legacyLimits)
            put("letter_dashboard_practice_type", "Reading")
            put("letter_dashboard_sort_by_time", true)
        }

        try {
            backup.importPreferences(legacyBackup)

            val preferences = manager.appPreferences
            assertEquals(PreferencesDefaultHomeTab.Moji, preferences.defaultHomeTab.get())
            assertEquals(PreferencesMojiPracticeType.Reading, preferences.mojiDashboardPracticeType.get())
            assertTrue(preferences.mojiDashboardSortByTime.get())
            val targets = preferences.generalDashboardStudyTargets.get()
            assertEquals(false, targets[StudyTarget.MojiWriting.preferenceKey])
            assertEquals(true, targets[StudyTarget.MojiFlashcards.preferenceKey])
            assertEquals(false, targets[StudyTarget.VocabWriting.preferenceKey])

            val limitsManager = DefaultDailyLimitManager(preferences)
            val limits = limitsManager.getConfiguration()
            assertFalse(limits.isMojiLimitCombined)
            assertEquals(PracticeLimit(7, 71), limits.mojiCombinedLimit)
            assertEquals(PracticeLimit(2, 22), limits.mojiSeparatedLimit[MojiPracticeType.Writing])
            assertEquals(PracticeLimit(3, 33), limits.mojiSeparatedLimit[MojiPracticeType.Reading])
            assertEquals(legacyBackup, backup.exportPreferences())

            limitsManager.update(true, limits.copy(mojiCombinedLimit = PracticeLimit(9, 99)))
            val exported = backup.exportPreferences()
            assertEquals(JsonPrimitive("Letters"), exported["default_home_tab"])
            val exportedLimits = Json.parseToJsonElement(
                exported.getValue("daily_limit_configuration").jsonPrimitive.content
            ).jsonObject
            assertEquals(JsonPrimitive(false), exportedLimits["isLetterLimitCombined"])
            assertEquals(
                Json.parseToJsonElement("""{"new":9,"due":99}"""),
                exportedLimits["letterCombinedLimit"]
            )
            assertTrue("letterSeparatedLimit" in exportedLimits)
            assertEquals(PracticeLimit(9, 99), limitsManager.getConfiguration().mojiCombinedLimit)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun restoresLegacyInfoAndDeckNavigationArguments() {
        val infoJson = """
            {"type":"ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData.Letter","letter":"休"}
        """.trimIndent()
        val info = Json.decodeFromString<InfoScreenData>(infoJson)
        assertEquals(InfoScreenData.Moji("休"), info)
        assertEquals(Json.parseToJsonElement(infoJson), Json.parseToJsonElement(Json.encodeToString(info)))

        val pickerJson = """
            {"type":"ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.data.DeckPickerScreenConfiguration.Letters"}
        """.trimIndent()
        assertEquals(DeckPickerScreenConfiguration.Moji, Json.decodeFromString<DeckPickerScreenConfiguration>(pickerJson))

        val detailsJson = """
            {"type":"ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration.LetterDeck","deckId":3}
        """.trimIndent()
        assertEquals(
            DeckDetailsScreenConfiguration.MojiDeck(3),
            Json.decodeFromString<DeckDetailsScreenConfiguration>(detailsJson)
        )

        val editJson = """
            {"type":"ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration.LetterDeck.Edit","title":"Study","letterDeckId":3}
        """.trimIndent()
        val edit = Json.decodeFromString<DeckEditScreenConfiguration>(editJson)
        assertEquals(DeckEditScreenConfiguration.MojiDeck.Edit("Study", 3), edit)
        assertEquals(Json.parseToJsonElement(editJson), Json.parseToJsonElement(Json.encodeToString(edit)))
    }

    @Test
    fun restoresLegacyPracticeDestinationAndKeepsAndroidRouteNames() {
        val navigationJson = Json {
            serializersModule = SerializersModule {
                polymorphic(MainDestination::class) {
                    defaultMainDestinations.forEach { it.subclassRegisterer(this) }
                }
            }
        }
        val legacyPracticeJson = """
            {
                "type":"ua.syt0r.kanji.presentation.screen.main.MainDestination.LetterPractice",
                "configuration":{"practiceType":"Writing","cards":[{"letter":"休","deckId":3}]}
            }
        """.trimIndent()
        val destination = assertIs<MainDestination.MojiPractice>(
            navigationJson.decodeFromString<MainDestination>(legacyPracticeJson)
        )
        assertEquals(ScreenMojiPracticeType.Writing, destination.configuration.practiceType)
        assertEquals("休", destination.configuration.cards.single().moji)
        assertEquals(3L, destination.configuration.cards.single().deckId)
        assertEquals(
            Json.parseToJsonElement(legacyPracticeJson),
            Json.parseToJsonElement(navigationJson.encodeToString<MainDestination>(destination))
        )
        assertEquals("LetterPractice", MainDestination.MojiPractice::class.configuration().routeName)
        assertEquals("LettersDashboard", HomeScreenTab.MojiDashboard.routeName)
    }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(data.value)
        data.value = updated
        return updated
    }
}
