package ua.syt0r.kanji.presentation.screen.main.screen.deck_details

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ua.syt0r.kanji.core.RefreshableData
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.presentation.LifecycleAwareViewModel
import ua.syt0r.kanji.presentation.LifecycleState
import ua.syt0r.kanji.presentation.common.ScreenMojiPracticeType
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.DeckDetailsScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsListItem
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsVisibleData
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case.DeckDetailsGetConfigurationUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case.GetDeckDetailsVisibleDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case.SubscribeOnDeckDetailsDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case.SubscribeOnVocabDeckDetailsDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.use_case.UpdateDeckDetailsConfigurationUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration

class DeckDetailsViewModel(
    private val viewModelScope: CoroutineScope,
    private val subscribeOnMojiDataUseCase: SubscribeOnDeckDetailsDataUseCase,
    private val subscribeOnVocabDataUseCase: SubscribeOnVocabDeckDetailsDataUseCase,
    private val getConfigurationUseCase: DeckDetailsGetConfigurationUseCase,
    private val updateConfigurationUseCase: UpdateDeckDetailsConfigurationUseCase,
    private val getVisibleDataUseCase: GetDeckDetailsVisibleDataUseCase,
    private val analyticsManager: AnalyticsManager,
) : DeckDetailsScreenContract.ViewModel, LifecycleAwareViewModel {

    private lateinit var configuration: DeckDetailsScreenConfiguration
    private var visibleDataCache: DeckDetailsVisibleData? = null

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    override val lifecycleState: MutableStateFlow<LifecycleState> =
        MutableStateFlow(LifecycleState.Hidden)

    override fun loadData(configuration: DeckDetailsScreenConfiguration) {
        if (::configuration.isInitialized) return
        this.configuration = configuration

        when (configuration) {
            is DeckDetailsScreenConfiguration.MojiDeck -> {
                subscribeOnMojiDataUseCase(configuration, lifecycleState)
                    .onEach { it.applyToState() }
                    .launchIn(viewModelScope)
            }

            is DeckDetailsScreenConfiguration.VocabDeck -> {
                subscribeOnVocabDataUseCase(configuration, lifecycleState)
                    .onEach { it.applyToState() }
                    .launchIn(viewModelScope)
            }
        }

    }

    override fun getPracticeConfiguration(group: DeckDetailsListItem.Group): MainDestination.MojiPractice {
        val deckId = configuration.deckId
        val loadedState = state.value as ScreenState.Loaded

        val configuration = loadedState.configuration.value
        configuration as DeckDetailsConfiguration.MojiDeckConfiguration

        val characters = group.items.map { it.character }
        return MainDestination.MojiPractice(
            configuration = MojiPracticeScreenConfiguration(
                cards = characters.map { MojiPracticeScreenConfiguration.Card(it, deckId) },
                practiceType = when (configuration.practiceType) {
                    ScreenMojiPracticeType.Writing -> ScreenMojiPracticeType.Writing
                    ScreenMojiPracticeType.Reading -> ScreenMojiPracticeType.Reading
                }
            )
        )
    }

    override fun getMultiselectPracticeConfiguration(): MainDestination {
        val loadedState = state.value as ScreenState.Loaded
        val currentVisibleData = loadedState.visibleDataState.value

        val deckId = this.configuration.deckId
        return when (val configuration = loadedState.configuration.value) {

            is DeckDetailsConfiguration.MojiDeckConfiguration -> {

                val characters: List<String> = when (currentVisibleData) {
                    is DeckDetailsVisibleData.Items -> currentVisibleData.items.asSequence()
                        .filter { it.selected.value }
                        .map { it.data.character }
                        .toList()

                    is DeckDetailsVisibleData.Groups -> currentVisibleData.items.asSequence()
                        .filter { it.selected.value }
                        .flatMap { it.items }
                        .map { it.character }
                        .toList()

                    else -> error("Wrong visible data type")

                }

                MainDestination.MojiPractice(
                    configuration = MojiPracticeScreenConfiguration(
                        cards = characters.map {
                            MojiPracticeScreenConfiguration.Card(it, deckId)
                        },
                        practiceType = when (configuration.practiceType) {
                            ScreenMojiPracticeType.Writing -> ScreenMojiPracticeType.Writing
                            ScreenMojiPracticeType.Reading -> ScreenMojiPracticeType.Reading
                        }
                    )
                )
            }

            is DeckDetailsConfiguration.VocabDeckConfiguration -> {
                currentVisibleData as DeckDetailsVisibleData.Vocab
                val vocabPracticeScreenConfiguration = VocabPracticeScreenConfiguration(
                    cards = currentVisibleData.items.asSequence()
                        .filter { it.selected.value }
                        .map { it.data.card }
                        .map {
                            VocabPracticeScreenConfiguration.Card(
                                cardId = it.cardId,
                                deckId = it.deckId
                            )
                        }
                        .toList(),
                    practiceType = loadedState.configuration.value
                        .let { it as DeckDetailsConfiguration.VocabDeckConfiguration }
                        .practiceType
                )
                MainDestination.VocabPractice(vocabPracticeScreenConfiguration)
            }
        }
    }

    private suspend fun RefreshableData<out DeckDetailsData>.applyToState() {
        when (this) {
            is RefreshableData.Loading -> {
                _state.value = ScreenState.Loading
            }

            is RefreshableData.Loaded -> {

                when (value) {
                    is DeckDetailsData.MojiDeckData -> {
                        val configuration = mutableStateOf(
                            getConfigurationUseCase.mojiConfiguration()
                        )

                        snapshotFlow { configuration.value }
                            .onEach { updateConfigurationUseCase(it) }
                            .launchIn(viewModelScope)

                        _state.value = ScreenState.Loaded.Moji(
                            title = value.deckTitle,
                            items = value.items,
                            configuration = configuration,
                            isSelectionModeEnabled = mutableStateOf(false),
                            visibleDataState = derivedStateOf {
                                getVisibleDataUseCase(
                                    items = value.items,
                                    configuration = configuration.value,
                                    currentVisibleData = visibleDataCache
                                ).also { visibleDataCache = it }
                            },
                            sharableDeckData = value.sharableDeckData
                        )
                    }

                    is DeckDetailsData.VocabDeckData -> {
                        val configuration = mutableStateOf(
                            getConfigurationUseCase.vocabConfiguration()
                        )

                        snapshotFlow { configuration.value }
                            .onEach { updateConfigurationUseCase(it) }
                            .launchIn(viewModelScope)

                        _state.value = ScreenState.Loaded.Vocab(
                            title = value.deckTitle,
                            items = value.items,
                            configuration = configuration,
                            isSelectionModeEnabled = mutableStateOf(false),
                            visibleDataState = derivedStateOf<DeckDetailsVisibleData> {
                                getVisibleDataUseCase(
                                    items = value.items,
                                    configuration = configuration.value,
                                    currentVisibleData = visibleDataCache
                                ).also { visibleDataCache = it }
                            }
                        )
                    }
                }

            }
        }
    }

}