import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.user_data.database.KanjiDeckTransferMode
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightMojiPracticeRepository
import ua.syt0r.kanji.core.user_data.db.UserDataDatabase

class OfflineDatabaseMigrationTest {

    @Test
    fun freshDatabaseHasNoTextAnalysisTable() {
        withSqlDatabase { driver, database ->
            assertFalse("text_analysis" in tableNames(driver))
            assertTrue(tableNames(driver).containsAll(studyTables))
            assertEquals(UserDataDatabase.Schema.version, databaseVersion(driver))
            assertEquals(null, database.userDataQueries.getOnlyAllowSingleDeckPerKanji().executeAsOneOrNull())
            assertTrue("kanji_deck_transfer_settings" in tableNames(driver))
            assertEquals(null, database.userDataQueries.getPreferKanjiDeckCopy().executeAsOneOrNull())
        }
    }

    @Test
    fun migrationRemovesAnalysisHistoryAndPreservesStudyData() {
        withSqlDatabase { driver, database ->
            driver.execute(null, """
                CREATE TABLE text_analysis (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    translation TEXT NOT NULL,
                    annotatedTextJson TEXT NOT NULL
                )
            """.trimIndent(), 0)
            driver.execute(null, "INSERT INTO text_analysis VALUES (1, 'sample', 100, 'translation', '[]')", 0)
            insertStudyRecords(driver)
            driver.execute(null, "DROP TABLE letter_deck_settings", 0)
            driver.execute(null, "DROP TABLE kanji_deck_transfer_settings", 0)
            driver.execute(null, "PRAGMA user_version = 12", 0)

            val before = studyRecords(database)
            UserDataDatabase.Schema.migrate(driver, 12, UserDataDatabase.Schema.version).value

            assertFalse("text_analysis" in tableNames(driver))
            assertEquals(before, studyRecords(database))
            assertEquals(UserDataDatabase.Schema.version, databaseVersion(driver))
            val queries = database.userDataQueries
            assertEquals(null, queries.getOnlyAllowSingleDeckPerKanji().executeAsOneOrNull())
            assertTrue("kanji_deck_transfer_settings" in tableNames(driver))
            assertEquals(null, queries.getPreferKanjiDeckCopy().executeAsOneOrNull())
            queries.setOnlyAllowSingleDeckPerKanji(1L)
            assertEquals(1L, UserDataDatabase(driver).userDataQueries.getOnlyAllowSingleDeckPerKanji().executeAsOne())
            queries.setOnlyAllowSingleDeckPerKanji(0L)
            assertEquals(0L, queries.getOnlyAllowSingleDeckPerKanji().executeAsOne())
        }
    }

    @Test
    fun originalVersion13ImportPreservesStudyDataAndDefaultsToMove() = withDatabase { manager, driver ->
        val database = UserDataDatabase(driver)
        insertStudyRecords(driver)
        driver.execute(null, "DROP TABLE letter_deck_settings", 0)
        driver.execute(null, "DROP TABLE kanji_deck_transfer_settings", 0)
        driver.execute(null, "PRAGMA user_version = 13", 0)
        val before = studyRecords(database)

        UserDataDatabase.Schema.migrate(driver, 13, UserDataDatabase.Schema.version).value

        assertEquals(before, studyRecords(database))
        assertEquals(UserDataDatabase.Schema.version, databaseVersion(driver))
        assertTrue("kanji_deck_transfer_settings" in tableNames(driver))
        assertEquals(null, database.userDataQueries.getPreferKanjiDeckCopy().executeAsOneOrNull())
        val options = SqlDelightMojiPracticeRepository(manager).getKanjiDeckTransferOptions()
        assertEquals(KanjiDeckTransferMode.Move, options.mode)
        assertTrue(options.canCopy)
    }

    @Test
    fun version14MigrationPreservesBothSingleDeckSettingsAndDefaultsToMove() {
        listOf(0L, 1L).forEach { singleDeckSetting ->
            withDatabase { manager, driver ->
                val database = UserDataDatabase(driver)
                insertStudyRecords(driver)
                database.userDataQueries.setOnlyAllowSingleDeckPerKanji(singleDeckSetting)
                driver.execute(null, "DROP TABLE kanji_deck_transfer_settings", 0)
                driver.execute(null, "PRAGMA user_version = 14", 0)
                val before = studyRecords(database)

                UserDataDatabase.Schema.migrate(driver, 14, UserDataDatabase.Schema.version).value

                assertEquals(before, studyRecords(database))
                assertEquals(UserDataDatabase.Schema.version, databaseVersion(driver))
                assertEquals(singleDeckSetting, database.userDataQueries.getOnlyAllowSingleDeckPerKanji().executeAsOne())
                assertTrue("kanji_deck_transfer_settings" in tableNames(driver))
                assertEquals(null, database.userDataQueries.getPreferKanjiDeckCopy().executeAsOneOrNull())
                val options = SqlDelightMojiPracticeRepository(manager).getKanjiDeckTransferOptions()
                assertEquals(KanjiDeckTransferMode.Move, options.mode)
                assertEquals(singleDeckSetting == 0L, options.canCopy)
            }
        }
    }

    private fun insertStudyRecords(driver: JdbcSqliteDriver) {
        driver.execute(null, "INSERT INTO letter_deck VALUES (1, 'Personal study', 0)", 0)
        driver.execute(null, "INSERT INTO letter_deck_entry VALUES ('X', 1)", 0)
        driver.execute(null, "INSERT INTO vocab_deck VALUES (1, 'Saved words', 0)", 0)
        driver.execute(null, "INSERT INTO vocab_deck_entry VALUES (1, 1, 'X', 'reading', 'meaning', 123)", 0)
        driver.execute(null, "INSERT INTO fsrs_card VALUES ('X', 0, 1, 2.5, 3.5, 1, 4, 100, 5)", 0)
        driver.execute(null, "INSERT INTO review_history VALUES ('X', 0, 100, 900, 3, 0, 1)", 0)
    }

    private fun studyRecords(database: UserDataDatabase): List<List<Any>> =
        database.userDataQueries.run {
            listOf(
                getAllMojiDecks().executeAsList(),
                getEntriesForMojiDeck(1).executeAsList(),
                getVocabDecks().executeAsList(),
                getVocabDeckEntries().executeAsList(),
                getFsrsCards().executeAsList(),
                getReviewHistory(Long.MIN_VALUE, Long.MAX_VALUE).executeAsList()
            )
        }

    private fun tableNames(driver: JdbcSqliteDriver): Set<String> = driver.executeQuery(
        null,
        "SELECT name FROM sqlite_master WHERE type = 'table'",
        { cursor ->
            QueryResult.Value(buildSet {
                while (cursor.next().value) add(cursor.getString(0)!!)
            })
        },
        0
    ).value

    private fun databaseVersion(driver: JdbcSqliteDriver): Long = driver.executeQuery(
        null,
        "PRAGMA user_version",
        { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0)!!)
        },
        0
    ).value

    private fun withSqlDatabase(block: (JdbcSqliteDriver, UserDataDatabase) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            UserDataDatabase.Schema.create(driver).value
            block(driver, UserDataDatabase(driver))
        } finally {
            driver.close()
        }
    }

    private val studyTables = setOf(
        "letter_deck", "letter_deck_entry", "vocab_deck", "vocab_deck_entry",
        "fsrs_card", "review_history"
    )
}
