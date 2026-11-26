package ua.syt0r.kanji.core.user_data.database

import org.koin.core.module.Module
import ua.syt0r.kanji.core.user_data.database.migration.UserDataDatabaseMigrationProvider
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightFsrsCardRepository
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightMojiPracticeRepository
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightVocabPracticeRepository
import ua.syt0r.kanji.core.user_data.database.use_case.DefaultUpdateLocalDataTimestampUseCase
import ua.syt0r.kanji.core.user_data.database.use_case.UpdateLocalDataTimestampUseCase
import ua.syt0r.kanji.core.user_data.preferences.SingleDeckPerKanjiPreference

fun Module.addUserDataDatabaseDefinitions() {

    single<UserDataDatabaseContract.Manager> {
        DefaultUserDataDatabaseManager(
            databasePlatformHandler = get(),
            updateLocalDataTimestampUseCase = get()
        )
    }

    single<UserDataDatabaseContract.MigrationProvider> {
        UserDataDatabaseMigrationProvider(
            preferences = get(),
            appDataRepository = get(),
            observable = get()
        )
    }

    single<UserDataDatabaseContract.MigrationObservable> {
        DefaultMigrationObservable()
    }

    single { SingleDeckPerKanjiPreference(databaseManager = get()) }

    single<MojiPracticeRepository> {
        SqlDelightMojiPracticeRepository(
            databaseManager = get()
        )
    }

    single<VocabPracticeRepository> {
        SqlDelightVocabPracticeRepository(
            databaseManager = get()
        )
    }

    factory<UpdateLocalDataTimestampUseCase> {
        DefaultUpdateLocalDataTimestampUseCase(
            appPreferences = get(),
            timeUtils = get()
        )
    }

    single<FsrsCardRepository> {
        SqlDelightFsrsCardRepository(
            userDataDatabaseManager = get()
        )
    }

    single<ReviewHistoryRepository> {
        SqlDelightReviewHistoryRepository(
            userDataDatabaseManager = get()
        )
    }



}
