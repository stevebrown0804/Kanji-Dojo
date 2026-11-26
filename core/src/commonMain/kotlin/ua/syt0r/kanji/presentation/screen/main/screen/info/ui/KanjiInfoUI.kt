package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.AppListItem
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
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
    onWordClick: (JapaneseWord) -> Unit
) {
    val radicalsExpanded = rememberSaveable { mutableStateOf(true) }

    MojiInfoLayout(
        mojiData = data,
        listState = listState,
        listSpacerState = listSpacerState,
        headingContent = {
            item { KanjiInfoHeading(data) }
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KanjiInfoHeading(data: MojiInfoData.Kanji) {
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
