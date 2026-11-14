package ua.syt0r.kanji.presentation.screen.main.screen.home

import kotlinx.coroutines.runBlocking
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesDefaultHomeTab

class HomeViewModel(
    appPreferences: PreferencesContract.AppPreferences
) : HomeScreenContract.ViewModel {
    override val defaultTab: HomeScreenTab = runBlocking {
        when (appPreferences.defaultHomeTab.get()) {
            PreferencesDefaultHomeTab.GeneralDashboard -> HomeScreenTab.GeneralDashboard
            PreferencesDefaultHomeTab.Letters -> HomeScreenTab.LettersDashboard
            PreferencesDefaultHomeTab.Vocab -> HomeScreenTab.VocabDashboard
        }
    }
}
