package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.user_data.database.KanjiDeckTransferMode
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.screen.main.screen.info.KanjiDeckRemovalPrompt
import ua.syt0r.kanji.presentation.screen.main.screen.info.KanjiDeckTransferDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.MojiInfoData

@Composable
fun MojiInfoUI(
    mojiData: MojiInfoData,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    onFuriganaClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit,
    deckRemovalPrompt: KanjiDeckRemovalPrompt?,
    onRequestDeckRemoval: (MojiDeck) -> Unit,
    onConfirmDeckRemoval: (KanjiDeckRemovalPrompt) -> Unit,
    onCancelDeckRemoval: (KanjiDeckRemovalPrompt) -> Unit,
    deckTransfer: KanjiDeckTransferDialog?,
    onRequestDeckTransfer: (MojiDeck) -> Unit,
    onSelectDeckTransferMode: (KanjiDeckTransferMode) -> Unit,
    onConfirmDeckTransfer: (Long) -> Unit,
    onDismissDeckTransfer: () -> Unit
) {
    when (mojiData) {
        is MojiInfoData.Kanji -> KanjiInfoUI(
            data = mojiData,
            listState = listState,
            listSpacerState = listSpacerState,
            onFuriganaClick = onFuriganaClick,
            onWordClick = onWordClick,
            deckRemovalPrompt = deckRemovalPrompt,
            onRequestDeckRemoval = onRequestDeckRemoval,
            onConfirmDeckRemoval = onConfirmDeckRemoval,
            onCancelDeckRemoval = onCancelDeckRemoval,
            deckTransfer = deckTransfer,
            onRequestDeckTransfer = onRequestDeckTransfer,
            onSelectDeckTransferMode = onSelectDeckTransferMode,
            onConfirmDeckTransfer = onConfirmDeckTransfer,
            onDismissDeckTransfer = onDismissDeckTransfer
        )
        is MojiInfoData.Kana -> KanaInfoUI(
            data = mojiData,
            listState = listState,
            listSpacerState = listSpacerState,
            onFuriganaClick = onFuriganaClick,
            onWordClick = onWordClick
        )
    }
}
