package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DefaultDeleteDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DefaultLoadDeckEditMojiDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DefaultLoadDeckEditVocabDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DefaultSaveDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DefaultValidateDeckCharactersUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.DeleteDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.LoadDeckEditMojiDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.LoadDeckEditVocabDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.SaveDeckUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case.ValidateDeckCharactersUseCase

val deckEditScreenModule = module {

    factory<LoadDeckEditMojiDataUseCase> {
        DefaultLoadDeckEditMojiDataUseCase(
            mojiPracticeRepository = get(),
            appDataRepository = get()
        )
    }

    factory<LoadDeckEditVocabDataUseCase> {
        DefaultLoadDeckEditVocabDataUseCase(
            practiceRepository = get(),
            appDataRepository = get(),
            vocabCardResolver = get()
        )
    }

    factory<ValidateDeckCharactersUseCase> {
        DefaultValidateDeckCharactersUseCase(
            appDataRepository = get()
        )
    }

    factory<SaveDeckUseCase> {
        DefaultSaveDeckUseCase(
            mojiPracticeRepository = get(),
            vocabPracticeRepository = get()
        )
    }

    factory<DeleteDeckUseCase> {
        DefaultDeleteDeckUseCase(
            mojiPracticeRepository = get(),
            vocabPracticeRepository = get()
        )
    }

    multiplatformViewModel<DeckEditScreenContract.ViewModel> {
        DeckEditViewModel(
            viewModelScope = it.component1(),
            loadDeckEditMojiDataUseCase = get(),
            loadDeckEditVocabDataUseCase = get(),
            validateDeckCharactersUseCase = get(),
            mojiPracticeRepository = get(),
            saveDeckUseCase = get(),
            deleteDeckUseCase = get(),
            analyticsManager = get()
        )
    }

}
