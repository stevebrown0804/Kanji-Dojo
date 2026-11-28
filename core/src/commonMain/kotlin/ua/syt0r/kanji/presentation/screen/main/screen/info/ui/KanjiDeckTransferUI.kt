package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.user_data.database.KanjiDeckTransferMode
import ua.syt0r.kanji.dialog_cancel
import ua.syt0r.kanji.info_kanji_decks_transfer_copy
import ua.syt0r.kanji.info_kanji_decks_transfer_failed
import ua.syt0r.kanji.info_kanji_decks_transfer_move
import ua.syt0r.kanji.info_kanji_decks_transfer_title
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.KanjiDeckTransferDialog

@Composable
internal fun KanjiDeckTransferUI(
    dialog: KanjiDeckTransferDialog,
    onSelectMode: (KanjiDeckTransferMode) -> Unit,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDeckId by rememberSaveable(dialog.sourceDeck.id) { mutableStateOf<Long?>(null) }
    val selectedDeck = dialog.destinations.firstOrNull { it.id == selectedDeckId }
    val moveLabel = stringResource(Res.string.info_kanji_decks_transfer_move)
    val copyLabel = stringResource(Res.string.info_kanji_decks_transfer_copy)

    MultiplatformDialog(
        onDismissRequest = { if (!dialog.isSubmitting) onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 500.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.info_kanji_decks_transfer_title),
                style = MaterialTheme.typography.titleLarge
            )

            val modesEnabled = dialog.canCopy && !dialog.isSubmitting
            val disabledFill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            val disabledLabel = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            val modeColors = SegmentedButtonDefaults.colors(
                disabledActiveContainerColor = disabledFill,
                disabledActiveContentColor = disabledLabel,
                disabledActiveBorderColor = disabledFill,
                disabledInactiveContentColor = disabledLabel,
                disabledInactiveBorderColor = disabledFill
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = dialog.mode == KanjiDeckTransferMode.Move,
                    onClick = { onSelectMode(KanjiDeckTransferMode.Move) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    enabled = modesEnabled,
                    colors = modeColors
                ) {
                    Text(moveLabel)
                }
                SegmentedButton(
                    selected = dialog.mode == KanjiDeckTransferMode.Copy,
                    onClick = { onSelectMode(KanjiDeckTransferMode.Copy) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = modesEnabled,
                    colors = modeColors
                ) {
                    Text(copyLabel)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                items(dialog.destinations, key = { it.id }) { deck ->
                    val selected = deck.id == selectedDeck?.id
                    val colors = MaterialTheme.colorScheme
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (selected) colors.secondaryContainer else Color.Transparent)
                            .selectable(
                                selected = selected,
                                enabled = !dialog.isSubmitting,
                                onClick = { selectedDeckId = deck.id }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = deck.name,
                            color = if (selected) colors.onSecondaryContainer else colors.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (dialog.failed) {
                Text(
                    text = stringResource(Res.string.info_kanji_decks_transfer_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, enabled = !dialog.isSubmitting) {
                    Text(stringResource(Res.string.dialog_cancel))
                }
                Button(
                    onClick = { selectedDeck?.let { onConfirm(it.id) } },
                    enabled = selectedDeck != null && !dialog.isSubmitting
                ) {
                    Text(if (dialog.mode == KanjiDeckTransferMode.Move) moveLabel else copyLabel)
                }
            }
        }
    }
}
