package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.ExtraListSpacerState
import ua.syt0r.kanji.presentation.common.ExtraSpacer
import ua.syt0r.kanji.presentation.common.PaginateableState
import ua.syt0r.kanji.presentation.common.PaginationLoadLaunchedEffect
import ua.syt0r.kanji.presentation.common.collectAsState
import ua.syt0r.kanji.presentation.common.trackList
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.dialog.SaveWordDialog
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.info.MojiInfoData

internal class MojiInfoExamplesState(
    val vocab: PaginateableState<JapaneseWord>,
    val sentences: PaginateableState<Sentence>,
    val vocabExpanded: MutableState<Boolean>,
    val sentencesExpanded: MutableState<Boolean>,
    val addWordToDeck: (JapaneseWord) -> Unit
)

@Composable
internal fun MojiInfoLayout(
    mojiData: MojiInfoData,
    listState: LazyListState,
    listSpacerState: ExtraListSpacerState,
    headingContent: LazyListScope.() -> Unit,
    examplesContent: LazyListScope.(MojiInfoExamplesState) -> Unit
) {
    var wordToAddToDeck by remember { mutableStateOf<JapaneseWord?>(null) }
    wordToAddToDeck?.let {
        SaveWordDialog(
            word = it,
            onDismissRequest = { wordToAddToDeck = null }
        )
    }

    val vocabExpanded = rememberSaveable { mutableStateOf(true) }
    val sentencesExpanded = rememberSaveable { mutableStateOf(true) }

    PaginationLoadLaunchedEffect(
        listState = listState,
        prefetchDistance = InfoScreenContract.ListPrefetchDistance,
        paginateableToExpandedStateList = listOf(
            mojiData.vocab to vocabExpanded,
            mojiData.sentences to sentencesExpanded
        )
    )

    val examples = MojiInfoExamplesState(
        vocab = mojiData.vocab.collectAsState(),
        sentences = mojiData.sentences.collectAsState(),
        vocabExpanded = vocabExpanded,
        sentencesExpanded = sentencesExpanded,
        addWordToDeck = { wordToAddToDeck = it }
    )

    if (LocalOrientation.current == Orientation.Portrait) {
        LazyColumn(
            state = listState,
            modifier = Modifier.trackList(listSpacerState)
        ) {
            headingContent()
            examplesContent(examples)
            listSpacerState.ExtraSpacer(this)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .trackList(listSpacerState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                headingContent()
                listSpacerState.ExtraSpacer(this)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                examplesContent(examples)
                listSpacerState.ExtraSpacer(this)
            }
        }
    }
}
