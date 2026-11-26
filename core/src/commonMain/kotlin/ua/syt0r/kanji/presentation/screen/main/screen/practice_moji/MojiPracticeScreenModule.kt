package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.DefaultGetMojiPracticeConfigurationUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.DefaultGetMojiPracticeQueueDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.DefaultGetMojiPracticeQueueItemDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.DefaultGetMojiPracticeReviewStateUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.DefaultUpdateMojiPracticeConfigurationUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeConfigurationUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeQueueDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeQueueItemDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeReviewStateUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.UpdateMojiPracticeConfigurationUseCase

val mojiPracticeScreenModule = module {

    factory<GetMojiPracticeConfigurationUseCase> {
        DefaultGetMojiPracticeConfigurationUseCase(
            practicePreferences = get(),
            srsCardRepository = get()
        )
    }

    factory<UpdateMojiPracticeConfigurationUseCase> {
        DefaultUpdateMojiPracticeConfigurationUseCase(
            practicePreferences = get()
        )
    }

    factory<GetMojiPracticeQueueDataUseCase> {
        DefaultGetMojiPracticeQueueDataUseCase(
            practicePreferences = get(),
            srsCardRepository = get(),
            configurationUpdateScope = it.component1()
        )
    }

    factory<GetMojiPracticeQueueItemDataUseCase> {
        DefaultGetMojiPracticeQueueItemDataUseCase(
            appDataRepository = get()
        )
    }

    factory<GetMojiPracticeReviewStateUseCase> {
        DefaultGetMojiPracticeReviewStateUseCase(
            characterWriterCoroutineScope = it.component1()
        )
    }

    factory<MojiPracticeQueue>(named<MojiPracticeScreenContract>()) {
        DefaultMojiPracticeQueue(
            coroutineScope = it.component1(),
            timeUtils = get(),
            srsCardRepository = get(),
            srsScheduler = get(),
            getQueueItemDataUseCase = get(),
            reviewHistoryRepository = get(),
            analyticsManager = get()
        )
    }

    multiplatformViewModel<MojiPracticeScreenContract.ViewModel> {
        MojiPracticeViewModel(
            viewModelScope = it.component1(),
            getConfigurationUseCase = get(),
            updateConfigurationUseCase = get(),
            getQueueDataUseCase = get { it },
            practiceQueue = get(named<MojiPracticeScreenContract>()) { it },
            getReviewStateUseCase = get { it },
            analyticsManager = get(),
            kanaTtsManager = get()
        )
    }

    single<MojiPracticeScreenContract.Content> { DefaultMojiPracticeScreenContent }

}