package ua.syt0r.kanji.presentation.screen.main.screen.info.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.info_kanji_decks_label
import ua.syt0r.kanji.info_kanji_decks_none

@Composable
internal fun KanjiInfoDeckMembership(decks: List<MojiDeck>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.info_kanji_decks_label),
            style = MaterialTheme.typography.bodyMedium
        )
        if (decks.isEmpty()) {
            Text(
                text = stringResource(Res.string.info_kanji_decks_none),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            decks.forEach { deck ->
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
