package ua.syt0r.kanji.presentation.dialog

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.user_data.database.LetterDeckConflict
import ua.syt0r.kanji.core.user_data.database.LetterDeckConflictResolution
import ua.syt0r.kanji.letter_deck_conflict_leave
import ua.syt0r.kanji.letter_deck_conflict_message
import ua.syt0r.kanji.letter_deck_conflict_move
import ua.syt0r.kanji.presentation.common.MultiplatformDialog

class LetterDeckConflictPrompt {

    internal data class Request(
        val conflict: LetterDeckConflict,
        val decision: CompletableDeferred<LetterDeckConflictResolution>
    )

    internal var request: Request? by mutableStateOf(null)
        private set

    suspend fun choose(conflict: LetterDeckConflict): LetterDeckConflictResolution {
        check(request == null)
        val decision = CompletableDeferred<LetterDeckConflictResolution>()
        request = Request(conflict, decision)
        try {
            return decision.await()
        } finally {
            request = null
        }
    }

    suspend fun save(
        initialResolutions: Map<String, LetterDeckConflictResolution> = emptyMap(),
        action: suspend (Map<String, LetterDeckConflictResolution>) -> List<LetterDeckConflict>
    ) {
        val resolutions = initialResolutions.toMutableMap()
        while (true) {
            val conflicts = action(resolutions)
            if (conflicts.isEmpty()) return
            for (conflict in conflicts) {
                resolutions[conflict.character] = choose(conflict)
            }
        }
    }
}

@Composable
fun LetterDeckConflictDialog(prompt: LetterDeckConflictPrompt) {
    val request = prompt.request ?: return
    val conflict = request.conflict
    val leave: () -> Unit = { request.decision.complete(LetterDeckConflictResolution.Leave) }

    MultiplatformDialog(
        onDismissRequest = leave,
        title = { Text(conflict.character) },
        content = {
            Text(
                stringResource(
                    Res.string.letter_deck_conflict_message,
                    conflict.decks.joinToString { it.name }
                )
            )
        },
        buttons = {
            TextButton(onClick = leave) {
                Text(stringResource(Res.string.letter_deck_conflict_leave))
            }
            TextButton(
                onClick = {
                    val sourceDeckIds = conflict.decks.map { it.id }.toSet()
                    request.decision.complete(LetterDeckConflictResolution.Move(sourceDeckIds))
                }
            ) {
                Text(stringResource(Res.string.letter_deck_conflict_move))
            }
        }
    )
}
