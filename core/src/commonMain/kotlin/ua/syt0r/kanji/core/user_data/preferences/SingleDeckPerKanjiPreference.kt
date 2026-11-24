package ua.syt0r.kanji.core.user_data.preferences

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onSubscription
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract

class SingleDeckPerKanjiPreference(
    private val databaseManager: UserDataDatabaseContract.Manager
) {

    private val updates = MutableSharedFlow<Unit>(replay = 1)
    val values = merge(
        databaseManager.databaseChangeEvents.onSubscription { emit(Unit) },
        updates
    ).map { get() }.distinctUntilChanged()

    suspend fun get(): Boolean = databaseManager.readTransaction {
        getOnlyAllowSingleDeckPerKanji().executeAsOneOrNull() == 1L
    }

    suspend fun set(enabled: Boolean) {
        databaseManager.writeTransaction {
            setOnlyAllowSingleDeckPerKanji(if (enabled) 1L else 0L)
        }
        updates.emit(Unit)
    }

}
