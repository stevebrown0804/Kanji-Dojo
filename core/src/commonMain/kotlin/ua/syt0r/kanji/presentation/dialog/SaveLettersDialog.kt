package ua.syt0r.kanji.presentation.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.common.ui.FilledTextField
import ua.syt0r.kanji.save_letters_dialog_contains
import ua.syt0r.kanji.save_letters_dialog_title
import ua.syt0r.kanji.save_word_dialog_button_add
import ua.syt0r.kanji.save_word_dialog_button_cancel
import ua.syt0r.kanji.save_word_dialog_completed_state_message
import ua.syt0r.kanji.save_word_dialog_contains_hint
import ua.syt0r.kanji.save_word_dialog_create_deck_button
import ua.syt0r.kanji.save_word_dialog_create_deck_title_hint
import ua.syt0r.kanji.save_word_dialog_saving_state_message

@Composable
fun SaveLettersDialog(
    letters: List<String>,
    onSaved: () -> Unit,
    onDismissRequest: () -> Unit
) {

    val dialogState = rememberDialogState(letters = letters)

    MultiplatformDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.save_letters_dialog_title)) },
        paddedContent = false,
        content = {
            AnimatedContent(
                targetState = dialogState.state.value,
                modifier = Modifier.fillMaxWidth()
            ) {
                DialogContent(
                    state = it,
                    onSaved = {
                        onDismissRequest()
                        onSaved()
                    },
                    createNewDeck = { dialogState.createNewDeck() }
                )
            }
        },
        buttons = {
            TextButton(onDismissRequest) {
                Text(stringResource(Res.string.save_word_dialog_button_cancel))
            }
            val isAddButtonEnabled = remember {
                derivedStateOf {
                    dialogState.state.value.let {
                        (it is DialogState.SelectingDeck && it.selectedDeck.value != null) ||
                                (it is DialogState.CreateNewDeck && it.title.value.isNotEmpty())
                    }
                }
            }
            TextButton(
                onClick = { dialogState.save() },
                enabled = isAddButtonEnabled.value
            ) {
                Text(
                    text = stringResource(Res.string.save_word_dialog_button_add)
                )
            }
        }
    )

    LetterDeckConflictDialog(dialogState.letterDeckConflictPrompt)

}

@Composable
private fun DialogContent(
    state: DialogState,
    onSaved: () -> Unit,
    createNewDeck: () -> Unit
) {
    when (state) {
        DialogState.Loading -> {
            CircularProgressIndicator(Modifier.fillMaxWidth().wrapContentWidth())
        }

        is DialogState.SelectingDeck -> {
            Column {
                AppListItem(
                    headlineContent = {
                        Text(stringResource(Res.string.save_word_dialog_create_deck_button))
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    onClick = createNewDeck,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.decks.forEach { deck ->
                    AppListItem(
                        headlineContent = { Text(deck.title) },
                        supportingContent = if (deck.alreadyContainsLetters.isNotEmpty()) {
                            {
                                Text(
                                    text = stringResource(
                                        Res.string.save_letters_dialog_contains,
                                        deck.alreadyContainsLetters.joinToString()
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            null
                        },
                        trailingContent = {
                            if (deck.id == state.selectedDeck.value?.id)
                                Icon(Icons.Default.Check, null)
                        },
                        onClick = { state.selectedDeck.value = deck },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        is DialogState.CreateNewDeck -> {
            AppListItem(
                headlineContent = {
                    FilledTextField(
                        value = state.title.value,
                        onValueChange = { state.title.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        hintContent = {
                            Text(
                                text = stringResource(Res.string.save_word_dialog_create_deck_title_hint),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            )
        }

        DialogState.Saving -> {
            Text(
                text = stringResource(Res.string.save_word_dialog_saving_state_message),
                modifier = Modifier.fillMaxWidth().wrapContentWidth()
            )
        }

        DialogState.Completed -> {
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.save_word_dialog_completed_state_message)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .background(MaterialTheme.extraColorScheme.success, CircleShape)
                        .size(24.dp)
                        .padding(2.dp)
                )
            }
            LaunchedEffect(Unit) {
                delay(600)
                onSaved()
            }
        }
    }
}

private sealed interface DialogState {

    data object Loading : DialogState

    data class SelectingDeck(
        val decks: List<SaveLetterDeckInfo>,
        val selectedDeck: MutableState<SaveLetterDeckInfo?>
    ) : DialogState

    data class CreateNewDeck(
        val title: MutableState<String>
    ) : DialogState

    data object Saving : DialogState
    data object Completed : DialogState

}

private data class SaveLetterDeckInfo(
    val id: Long,
    val title: String,
    val alreadyContainsLetters: Set<String>
)

@Composable
private fun rememberDialogState(letters: List<String>): SaveLettersDialogState {
    val repository = koinInject<LetterPracticeRepository>()
    val coroutineScope = rememberCoroutineScope()
    return remember {
        SaveLettersDialogState(
            letters = letters,
            repository = repository,
            coroutineScope = coroutineScope
        )
    }
}

private class SaveLettersDialogState(
    private val letters: List<String>,
    private val repository: LetterPracticeRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _state = mutableStateOf<DialogState>(DialogState.Loading)
    val state: State<DialogState> = _state
    val letterDeckConflictPrompt = LetterDeckConflictPrompt()

    init {
        coroutineScope.launch {
            val lettersSet = letters.toSet()
            _state.value = DialogState.SelectingDeck(
                decks = repository.getDecks().map {
                    val deckLetters = repository.getDeckCharacters(it.id).toSet()
                    SaveLetterDeckInfo(
                        id = it.id,
                        title = it.name,
                        alreadyContainsLetters = lettersSet.intersect(deckLetters)
                    )
                },
                selectedDeck = mutableStateOf(null)
            )
        }
    }

    fun createNewDeck() {
        _state.value = DialogState.CreateNewDeck(
            title = mutableStateOf("")
        )
    }

    fun save() {
        val currentState = _state.value
        coroutineScope.launch {
            when (currentState) {
                is DialogState.CreateNewDeck -> {
                    _state.value = DialogState.Saving
                    val title = currentState.title.value
                    letterDeckConflictPrompt.save {
                        repository.createDeck(
                            title = title,
                            characters = letters,
                            conflictResolutions = it
                        )
                    }
                }

                is DialogState.SelectingDeck -> {
                    val deck = currentState.selectedDeck.value ?: return@launch
                    _state.value = DialogState.Saving
                    letterDeckConflictPrompt.save {
                        repository.updateDeck(
                            id = deck.id,
                            title = deck.title,
                            charactersToAdd = letters,
                            charactersToRemove = emptyList(),
                            conflictResolutions = it
                        )
                    }
                }

                else -> return@launch
            }
            _state.value = DialogState.Completed
        }
    }

}
