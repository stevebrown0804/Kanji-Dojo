package ua.syt0r.kanji.presentation.screen.main.screen.info

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.InfoLoadMojiStateUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.info.use_case.InfoLoadVocabStateUseCase

val infoScreenModule = module {

    factory<InfoScreenContract.LoadMojiStateUseCase> {
        InfoLoadMojiStateUseCase(
            appDataRepository = get(),
            characterClassifier = get(),
            mojiPracticeRepository = get(),
            analyticsManager = get()
        )
    }

    factory<InfoScreenContract.LoadVocabStateUseCase> {
        InfoLoadVocabStateUseCase(
            appDataRepository = get()
        )
    }

    multiplatformViewModel<InfoScreenContract.ViewModel> {
        InfoScreenViewModel(
            viewModelScope = it.component1(),
            screenData = it.component2(),
            loadMojiStateUseCase = get(),
            loadVocabStateUseCase = get(),
            analyticsManager = get()
        )
    }

}
