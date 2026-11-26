package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.resources.icon.Copy
import ua.syt0r.kanji.presentation.common.resources.icon.ExtraIcons
import ua.syt0r.kanji.presentation.common.theme.extraColorScheme
import ua.syt0r.kanji.presentation.common.ui.kanji.AnimatedKanji
import ua.syt0r.kanji.presentation.common.ui.kanji.KanjiBackground

@Composable
internal fun MojiInfoStrokeAnimation(strokes: List<Path>) {

    Column {

        Card(
            modifier = Modifier.size(120.dp),
            elevation = CardDefaults.elevatedCardElevation()
        ) {

            Box(modifier = Modifier.fillMaxSize()) {

                KanjiBackground(Modifier.fillMaxSize())

                AnimatedKanji(
                    strokes = strokes,
                    modifier = Modifier.fillMaxSize()
                )

            }

        }

        Text(
            text = resolveString { info.strokesMessage(strokes.size) },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(top = 4.dp)
                .align(Alignment.CenterHorizontally)
        )

    }

}

private const val CopyAnimationDuration = 800L

@Composable
internal fun MojiInfoCopyButton(
    copyData: String,
    modifier: Modifier = Modifier
) {

    val clipboardManager = LocalClipboardManager.current
    var copying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { copying }
            .filter { it }
            .collect {
                delay(CopyAnimationDuration)
                copying = false
            }
    }

    IconButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(copyData))
            copying = true
        },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = copying,
            transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() }
        ) {
            if (it) Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .background(MaterialTheme.extraColorScheme.success, CircleShape)
                    .size(24.dp)
                    .padding(2.dp)
            )
            else Icon(ExtraIcons.Copy, null)
        }
    }

}
