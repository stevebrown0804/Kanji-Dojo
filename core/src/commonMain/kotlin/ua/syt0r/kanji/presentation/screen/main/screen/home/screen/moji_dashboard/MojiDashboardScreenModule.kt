package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.use_case.MojiDashboardLoadDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.use_case.MojiDashboardUpdateSortUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.moji_dashboard.use_case.MergeMojiDecksUseCase

val mojiDashboardScreenModule = module {

    factory<MojiDashboardScreenContract.LoadDataUseCase> {
        MojiDashboardLoadDataUseCase(
            srsManager = get(),
            timeUtils = get()
        )
    }

    factory<MojiDashboardScreenContract.MergeDecksUseCase> {
        MergeMojiDecksUseCase(repository = get())
    }

    factory<MojiDashboardScreenContract.UpdateSortUseCase> {
        MojiDashboardUpdateSortUseCase(
            appPreferences = get(),
            practiceRepository = get()
        )
    }

    multiplatformViewModel<MojiDashboardScreenContract.ViewModel> {
        MojiDashboardViewModel(
            viewModelScope = it.component1(),
            loadDataUseCase = get(),
            mergeDecksUseCase = get(),
            updateSortUseCase = get(),
            appPreferences = get(),
            analyticsManager = get()
        )
    }

}