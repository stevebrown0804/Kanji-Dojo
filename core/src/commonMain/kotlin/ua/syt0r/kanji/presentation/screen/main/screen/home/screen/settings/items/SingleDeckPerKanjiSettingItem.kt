package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.user_data.preferences.SingleDeckPerKanjiPreference
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsSwitchRow

class SingleDeckPerKanjiSettingItem(
    private val preference: SingleDeckPerKanjiPreference
) : SettingsScreenContract.ConfigurableListItem {

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var checked: StateFlow<Boolean>

    override suspend fun prepare(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
        checked = preference.values.stateIn(coroutineScope)
    }

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingsSwitchRow(
            title = resolveString { settings.onlyAllowSingleDeckPerKanji },
            checked = checked.collectAsState().value,
            onCheckedChange = { enabled ->
                coroutineScope.launch { preference.set(enabled) }
            }
        )
    }

}
