package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.info_kanji_decks_delete
import ua.syt0r.kanji.info_kanji_decks_label
import ua.syt0r.kanji.info_kanji_decks_none

@Composable
internal fun KanjiInfoDeckMembership(decks: List<MojiDeck>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.info_kanji_decks_label),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 24.sp)
        )
        if (decks.isEmpty()) {
            Text(
                text = stringResource(Res.string.info_kanji_decks_none),
                style = MaterialTheme.typography.titleLarge
            )
        } else {
            decks.forEach { deck ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deck.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.info_kanji_decks_delete, deck.name)
                        )
                    }
                }
            }
        }
    }
}
