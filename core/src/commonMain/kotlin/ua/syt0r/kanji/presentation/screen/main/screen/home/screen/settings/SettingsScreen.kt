package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

@Composable
fun SettingsScreen(
    mainNavigationState: MainNavigationState,
    viewModel: SettingsScreenContract.ViewModel = getMultiplatformViewModel()
) {

    SettingsScreenUI(
        state = viewModel.state.collectAsState(),
        onBackupButtonClick = {
            mainNavigationState.navigate(MainDestination.Backup)
        },
        onAboutButtonClick = {
            mainNavigationState.navigate(MainDestination.About)
        },
        loadedContent = { screenState ->
            screenState.items.forEach { it.content(mainNavigationState) }
        }
    )

}
