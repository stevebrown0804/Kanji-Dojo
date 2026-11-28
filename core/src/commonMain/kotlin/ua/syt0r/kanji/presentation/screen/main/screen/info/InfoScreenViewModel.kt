package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.srs.MojiSrsManager
import ua.syt0r.kanji.core.user_data.database.KanjiDeckTransferMode
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.core.user_data.database.MojiPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract.ScreenState

class InfoScreenViewModel(
    private val viewModelScope: CoroutineScope,
    screenData: InfoScreenData,
    private val loadMojiStateUseCase: InfoScreenContract.LoadMojiStateUseCase,
    private val loadVocabStateUseCase: InfoScreenContract.LoadVocabStateUseCase,
    private val analyticsManager: AnalyticsManager,
    private val mojiPracticeRepository: MojiPracticeRepository,
    private val mojiSrsManager: MojiSrsManager
) : InfoScreenContract.ViewModel {

    override val state = mutableStateOf<ScreenState>(ScreenState.Loading)
    private val activeDeckRemovalPrompt = mutableStateOf<KanjiDeckRemovalPrompt?>(null)
    override val deckRemovalPrompt: State<KanjiDeckRemovalPrompt?> = activeDeckRemovalPrompt
    private val activeDeckTransfer = mutableStateOf<KanjiDeckTransferDialog?>(null)
    override val deckTransfer: State<KanjiDeckTransferDialog?> = activeDeckTransfer
    private val deckTransferModeMutex = Mutex()
    private var loadingDeckTransfer = false

    init {
        viewModelScope.launch {
            state.value = withContext(Dispatchers.IO) { screenData.toState() }
        }
    }

    override fun requestDeckRemoval(deck: MojiDeck) {
        if (activeDeckRemovalPrompt.value == null &&
            activeDeckTransfer.value == null &&
            !loadingDeckTransfer
        ) {
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

    override fun requestDeckTransfer(deck: MojiDeck) {
        if (activeDeckTransfer.value != null || loadingDeckTransfer || deckRemovalPrompt.value != null) return
        val data = (state.value as? ScreenState.Loaded.Moji)?.data as? MojiInfoData.Kanji ?: return
        val membership = data.deckMembership.value
        if (membership.deckCount < 2 || membership.containingDecks.none { it.id == deck.id }) return

        loadingDeckTransfer = true
        viewModelScope.launch {
            try {
                val destinations = withContext(Dispatchers.IO) {
                    mojiSrsManager.getDecks().decks
                        .filter { it.id != deck.id }
                        .map { MojiDeck(it.id, it.title, it.position) }
                }
                if (destinations.isNotEmpty()) {
                    val options = deckTransferModeMutex.withLock {
                        mojiPracticeRepository.getKanjiDeckTransferOptions()
                    }
                    activeDeckTransfer.value = KanjiDeckTransferDialog(
                        sourceDeck = deck,
                        destinations = destinations,
                        mode = options.mode,
                        canCopy = options.canCopy
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Logger.e(exception.stackTraceToString())
                activeDeckTransfer.value = KanjiDeckTransferDialog(
                    sourceDeck = deck,
                    destinations = emptyList(),
                    mode = KanjiDeckTransferMode.Move,
                    canCopy = false,
                    failed = true
                )
            } finally {
                loadingDeckTransfer = false
            }
        }
    }

    override fun selectDeckTransferMode(mode: KanjiDeckTransferMode) {
        val dialog = activeDeckTransfer.value ?: return
        if (!dialog.canCopy || dialog.isSubmitting || dialog.mode == mode) return
        val updatedDialog = dialog.copy(mode = mode, failed = false)
        activeDeckTransfer.value = updatedDialog

        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                deckTransferModeMutex.withLock {
                    mojiPracticeRepository.setKanjiDeckTransferMode(mode)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Logger.e(exception.stackTraceToString())
                if (activeDeckTransfer.value === updatedDialog) {
                    activeDeckTransfer.value = updatedDialog.copy(failed = true)
                }
            }
        }
    }

    override fun confirmDeckTransfer(destinationDeckId: Long) {
        val dialog = activeDeckTransfer.value ?: return
        if (dialog.isSubmitting || dialog.destinations.none { it.id == destinationDeckId }) return
        val data = (state.value as? ScreenState.Loaded.Moji)?.data as? MojiInfoData.Kanji ?: return
        activeDeckTransfer.value = dialog.copy(isSubmitting = true, failed = false)

        viewModelScope.launch {
            val completed = try {
                mojiPracticeRepository.transferKanjiBetweenDecks(
                    character = data.character,
                    sourceDeckId = dialog.sourceDeck.id,
                    destinationDeckId = destinationDeckId,
                    mode = dialog.mode
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Logger.e(exception.stackTraceToString())
                false
            }
            activeDeckTransfer.value = if (completed) null else dialog.copy(failed = true)
        }
    }

    override fun dismissDeckTransfer() {
        if (activeDeckTransfer.value?.isSubmitting == false) {
            activeDeckTransfer.value = null
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
