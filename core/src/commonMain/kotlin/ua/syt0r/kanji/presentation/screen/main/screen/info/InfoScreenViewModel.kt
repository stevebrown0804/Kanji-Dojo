package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.core.user_data.database.MojiPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract.ScreenState

class InfoScreenViewModel(
    private val viewModelScope: CoroutineScope,
    screenData: InfoScreenData,
    private val loadMojiStateUseCase: InfoScreenContract.LoadMojiStateUseCase,
    private val loadVocabStateUseCase: InfoScreenContract.LoadVocabStateUseCase,
    private val analyticsManager: AnalyticsManager,
    private val mojiPracticeRepository: MojiPracticeRepository
) : InfoScreenContract.ViewModel {

    override val state = mutableStateOf<ScreenState>(ScreenState.Loading)
    private val activeDeckRemovalPrompt = mutableStateOf<KanjiDeckRemovalPrompt?>(null)
    override val deckRemovalPrompt: State<KanjiDeckRemovalPrompt?> = activeDeckRemovalPrompt

    init {
        viewModelScope.launch {
            state.value = withContext(Dispatchers.IO) { screenData.toState() }
        }
    }

    override fun requestDeckRemoval(deck: MojiDeck) {
        if (activeDeckRemovalPrompt.value == null) {
            activeDeckRemovalPrompt.value = KanjiDeckRemovalPrompt(deck)
        }
    }

    override fun confirmDeckRemoval(prompt: KanjiDeckRemovalPrompt) {
        if (activeDeckRemovalPrompt.value != prompt || prompt.isSubmitting) return
        val data = (state.value as? ScreenState.Loaded.Moji)?.data as? MojiInfoData.Kanji ?: return
        activeDeckRemovalPrompt.value = prompt.copy(isSubmitting = true, failed = false)

        viewModelScope.launch {
            try {
                val completed = mojiPracticeRepository.tryRemoveMojiFromDeck(
                    character = data.character,
                    deckId = prompt.deck.id,
                    allowLastDeckRemoval = prompt.isLastDeckConfirmation
                )
                activeDeckRemovalPrompt.value = if (completed) {
                    null
                } else {
                    prompt.copy(isLastDeckConfirmation = true, failed = false)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Logger.e(exception.stackTraceToString())
                activeDeckRemovalPrompt.value = prompt.copy(failed = true)
            }
        }
    }

    override fun cancelDeckRemoval(prompt: KanjiDeckRemovalPrompt) {
        if (activeDeckRemovalPrompt.value == prompt && !prompt.isSubmitting) {
            activeDeckRemovalPrompt.value = null
        }
    }

    private suspend fun InfoScreenData.toState(): ScreenState {
        return when (this) {
            is InfoScreenData.Moji -> {
                if (moji.length == 1) {
                    reportMoji(moji)
                    loadMojiStateUseCase(moji, viewModelScope)
                } else {
                    loadVocabStateUseCase(asVocabData(), viewModelScope)
                }
            }

            is InfoScreenData.Vocab -> {
                loadVocabStateUseCase(this, viewModelScope)
            }
        }
    }

    private fun InfoScreenData.Moji.asVocabData(): InfoScreenData.Vocab {
        val kanjiReading: String?
        val kanaReading: String?

        when {
            moji.any { it.isKanji() } -> {
                kanjiReading = moji
                kanaReading = null
            }

            else -> {
                kanjiReading = null
                kanaReading = moji
            }
        }

        return InfoScreenData.Vocab(
            id = null,
            kanjiReading = kanjiReading,
            kanaReading = kanaReading
        )
    }

    private fun reportMoji(moji: String) {
        analyticsManager.sendEvent("kanji_info_open") { put("character", moji) }
    }

}
