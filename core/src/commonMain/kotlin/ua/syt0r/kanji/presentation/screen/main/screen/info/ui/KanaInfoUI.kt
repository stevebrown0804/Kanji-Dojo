package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.resolveString
import ua.syt0r.kanji.presentation.screen.main.screen.info.MojiInfoData
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableSentenceSection
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenExpandableVocabSection
import ua.syt0r.kanji.presentation.common.resources.string.resolveString

@Composable
fun KanaInfoUI(
    data: MojiInfoData.Kana,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    onFuriganaClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit
) {
    MojiInfoLayout(
        mojiData = data,
        listState = listState,
        listSpacerState = listSpacerState,
        headingContent = { item { KanaInfoHeading(data) } },
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KanaInfoHeading(data: MojiInfoData.Kana) {

    AppListItem(
        headlineContent = {

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    MojiInfoStrokeAnimation(data.strokes)

                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        val readings = data.reading.let {
                            if (it.alternative != null) listOf(it.nihonShiki) + it.alternative
                            else listOf(it.nihonShiki)
                        }

                        val messages = listOf(
                            data.kanaSystem.resolveString(),
                            resolveString { info.romajiMessage(readings) }
                        )

                        Text(
                            text = messages.joinToString("\n"),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall
                        )

                        MojiInfoCopyButton(data.character)

                    }

                }

            }
        }
    )

}
