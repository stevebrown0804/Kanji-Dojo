package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.dialog_cancel
import ua.syt0r.kanji.info_kanji_decks_remove_abandon
import ua.syt0r.kanji.info_kanji_decks_remove_confirm
import ua.syt0r.kanji.info_kanji_decks_remove_deck_message
import ua.syt0r.kanji.info_kanji_decks_remove_failed
import ua.syt0r.kanji.info_kanji_decks_remove_last_deck_message
import ua.syt0r.kanji.info_kanji_decks_remove_proceed
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.screen.main.screen.info.KanjiDeckRemovalPrompt
import ua.syt0r.kanji.presentation.screen.main.screen.info.MojiInfoData
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableSentenceSection
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableVocabSection
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiRadicalUI
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiReadingsContainer
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableSection

@Composable
fun KanjiInfoUI(
    data: MojiInfoData.Kanji,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    onFuriganaClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit,
    deckRemovalPrompt: KanjiDeckRemovalPrompt? = null,
    onRequestDeckRemoval: (MojiDeck) -> Unit = {},
    onConfirmDeckRemoval: (KanjiDeckRemovalPrompt) -> Unit = {},
    onCancelDeckRemoval: (KanjiDeckRemovalPrompt) -> Unit = {}
) {
    val radicalsExpanded = rememberSaveable { mutableStateOf(true) }

    MojiInfoLayout(
        mojiData = data,
        listState = listState,
        listSpacerState = listSpacerState,
        headingContent = {
            item { KanjiInfoHeading(data, onRequestDeckRemoval) }
            val radicalsData = data.radicalsSectionData
            infoScreenExpandableSection(
                headerText = "Radicals",
                headerCount = radicalsData.radicals.size,
                expanded = radicalsExpanded,
                expandedContent = {
                    items(radicalsData.radicals) {
                        KanjiRadicalUI(
                            strokes = radicalsData.strokes,
                            radicalDetails = it,
                            onRadicalClick = onFuriganaClick
                        )
                    }
                }
            )
        },
        examplesContent = { examples ->
            infoScreenExpandableVocabSection(
                expanded = examples.vocabExpanded,
                paginateable = examples.vocab,
                onWordClick = onWordClick,
                onFuriganaClick = onFuriganaClick,
                addWordToVocabDeckClick = examples.addWordToDeck
            )
            infoScreenExpandableSentenceSection(
                expanded = examples.sentencesExpanded,
                paginateable = examples.sentences,
                onFuriganaClick = onFuriganaClick
            )
        }
    )

    deckRemovalPrompt?.let { prompt ->
        KanjiDeckRemovalDialog(
            prompt = prompt,
            onConfirm = { onConfirmDeckRemoval(prompt) },
            onCancel = { onCancelDeckRemoval(prompt) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KanjiInfoHeading(
    data: MojiInfoData.Kanji,
    onRequestDeckRemoval: (MojiDeck) -> Unit
) {
    val containingDecks by data.containingDecks.collectAsState()

    AppListItem(
        headlineContent = {
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
                ) {

                    MojiInfoStrokeAnimation(strokes = data.strokes)

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        val messages = listOfNotNull(
                            data.grade?.let { resolveString { info.gradeMessage(it) } },
                            data.jlptLevel?.let { resolveString { info.jlptMessage(it) } },
                            data.frequency?.let { resolveString { info.frequencyMessage(it) } }
                        )

                        if (messages.isNotEmpty()) {
                            Text(
                                text = messages.joinToString("\n"),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                    }

                    MojiInfoCopyButton(data.character)

                }

                KanjiInfoDeckMembership(
                    decks = containingDecks,
                    onRequestDeckRemoval = onRequestDeckRemoval,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (data.meanings.isNotEmpty()) {
                    Text(
                        text = data.meanings.joinToString(),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                KanjiReadingsContainer(
                    on = data.on,
                    kun = data.kun,
                    modifier = Modifier.fillMaxWidth()
                )

            }
        }
    )

}

@Composable
private fun KanjiDeckRemovalDialog(
    prompt: KanjiDeckRemovalPrompt,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val message = if (prompt.isLastDeckConfirmation) {
        stringResource(Res.string.info_kanji_decks_remove_last_deck_message)
    } else {
        stringResource(Res.string.info_kanji_decks_remove_deck_message, prompt.deck.name)
    }
    val cancelLabel = if (prompt.isLastDeckConfirmation) {
        Res.string.info_kanji_decks_remove_abandon
    } else {
        Res.string.dialog_cancel
    }
    val confirmLabel = if (prompt.isLastDeckConfirmation) {
        Res.string.info_kanji_decks_remove_confirm
    } else {
        Res.string.info_kanji_decks_remove_proceed
    }

    AlertDialog(
        onDismissRequest = { if (!prompt.isSubmitting) onCancel() },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message)
                if (prompt.failed) {
                    Text(
                        text = stringResource(Res.string.info_kanji_decks_remove_failed),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !prompt.isSubmitting) {
                Text(stringResource(cancelLabel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !prompt.isSubmitting) {
                Text(stringResource(confirmLabel))
            }
        }
    )
}
