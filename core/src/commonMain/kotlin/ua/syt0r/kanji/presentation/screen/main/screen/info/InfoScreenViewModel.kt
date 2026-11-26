package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract.ScreenState

class InfoScreenViewModel(
    private val viewModelScope: CoroutineScope,
    screenData: InfoScreenData,
    private val loadMojiStateUseCase: InfoScreenContract.LoadMojiStateUseCase,
    private val loadVocabStateUseCase: InfoScreenContract.LoadVocabStateUseCase,
    private val analyticsManager: AnalyticsManager
) : InfoScreenContract.ViewModel {

    override val state = mutableStateOf<ScreenState>(ScreenState.Loading)

    init {
        viewModelScope.launch {
            state.value = withContext(Dispatchers.IO) { screenData.toState() }
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
