package ua.syt0r.kanji.presentation.screen.main

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ua.syt0r.kanji.BuildConfig
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.emitWhenWithSubscribers
import ua.syt0r.kanji.core.user_data.database.DatabaseMigrationState
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.update_snackbar_action
import ua.syt0r.kanji.update_snackbar_title

class MainScreenViewModel(
    private val viewModelScope: CoroutineScope,
    appPreferences: PreferencesContract.AppPreferences,
    migrationObservable: UserDataDatabaseContract.MigrationObservable
) : MainContract.ViewModel {

    private val _notifications = MutableSharedFlow<MainSnackbarNotification>()
    override val notifications: SharedFlow<MainSnackbarNotification> = _notifications

    override val migrationState: StateFlow<DatabaseMigrationState> = migrationObservable.state
    override val showVersionChangeDialog: MutableState<Boolean> = mutableStateOf(false)

    init {
        viewModelScope.launch {
            val currentVersion = BuildConfig.versionName
            val showVersionNotification = appPreferences
                .lastAppVersionWhenChangesDialogShown.get() != currentVersion
            if (!showVersionNotification) return@launch

            appPreferences.lastAppVersionWhenChangesDialogShown.set(currentVersion)

            val notification = MainSnackbarNotification(
                message = getString(Res.string.update_snackbar_title),
                isError = false,
                actionLabel = getString(Res.string.update_snackbar_action),
                handleAction = {
                    showVersionChangeDialog.value = true
                    null
                }
            )
            _notifications.emitWhenWithSubscribers(notification)
        }
    }
}
