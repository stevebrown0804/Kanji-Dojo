package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.screen.main.screen.info.MojiInfoData

@Composable
fun MojiInfoUI(
    mojiData: MojiInfoData,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    onFuriganaClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit
) {
    when (mojiData) {
        is MojiInfoData.Kanji -> KanjiInfoUI(
            data = mojiData,
            listState = listState,
            listSpacerState = listSpacerState,
            onFuriganaClick = onFuriganaClick,
            onWordClick = onWordClick
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
