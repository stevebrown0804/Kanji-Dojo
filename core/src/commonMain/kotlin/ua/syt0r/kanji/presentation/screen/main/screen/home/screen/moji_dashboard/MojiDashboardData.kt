package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard

import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.MojiDeckDashboardItem


data class MojiDashboardScreenData(
    val items: List<MojiDeckDashboardItem>,
    val dailyLimitEnabled: Boolean
)
