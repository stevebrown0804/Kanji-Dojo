package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriter
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriterDecorations
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeItemData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeReviewState

@Composable
fun MojiPracticeWritingInputSection(
    state: State<MojiPracticeReviewState.Writing>,
    modifier: Modifier = Modifier
) {

    val writerState = remember { derivedStateOf { state.value.writerState.value } }

    CharacterWriterDecorations(
        modifier = modifier,
        state = writerState,
        showStartOver = state.value.itemData is MojiPracticeItemData.KanjiWritingData
    ) {

        val transition = updateTransition(
            targetState = writerState.value,
            label = "Different Characters Transition"
        )

        transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(),
                    initialContentExit = fadeOut()
                )
            }
        ) { currentWriterState ->

            CharacterWriter(
                state = currentWriterState,
                modifier = Modifier.fillMaxSize()
            )

        }

    }

}
