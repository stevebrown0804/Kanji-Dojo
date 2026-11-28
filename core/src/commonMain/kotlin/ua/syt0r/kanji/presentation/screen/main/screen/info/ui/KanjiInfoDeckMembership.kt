package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.info_kanji_decks_delete
import ua.syt0r.kanji.info_kanji_decks_label
import ua.syt0r.kanji.info_kanji_decks_move
import ua.syt0r.kanji.info_kanji_decks_none

@Composable
internal fun KanjiInfoDeckMembership(
    decks: List<MojiDeck>,
    onRequestDeckRemoval: (MojiDeck) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = if (decks.isEmpty()) 8.dp else 2.dp
    val viewConfiguration = LocalViewConfiguration.current
    val compactViewConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val minimumTouchTargetSize = DpSize(32.dp, 32.dp)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.info_kanji_decks_label),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp)
        )
        if (decks.isEmpty()) {
            Text(
                text = stringResource(Res.string.info_kanji_decks_none),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides 32.dp,
                LocalViewConfiguration provides compactViewConfiguration
            ) {
                Column {
                    decks.forEach { deck ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = deck.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                                    contentDescription = stringResource(Res.string.info_kanji_decks_move, deck.name)
                                )
                            }
                            IconButton(
                                onClick = { onRequestDeckRemoval(deck) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.info_kanji_decks_delete, deck.name)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
