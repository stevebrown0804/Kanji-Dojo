package ua.syt0r.kanji.core.user_data.database.sqldelight

import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.user_data.database.LetterDeck
import ua.syt0r.kanji.core.user_data.database.LetterDeckConflict
import ua.syt0r.kanji.core.user_data.database.LetterDeckConflictResolution
import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.core.user_data.database.ObservableUserDataRepository
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.userdata.db.UserDataQueries

class SqlDelightLetterPracticeRepository(
    databaseManager: UserDataDatabaseContract.Manager,
) : ObservableUserDataRepository(databaseManager), LetterPracticeRepository {

    override suspend fun createDeck(
        title: String,
        characters: List<String>,
        conflictResolutions: Map<String, LetterDeckConflictResolution>,
    ): List<LetterDeckConflict> = writeDeckWithConflicts {
        val charactersToLeave = if (onlyAllowSingleDeckPerKanji()) {
            resolveDeckConflicts(characters, conflictResolutions)
        } else {
            emptySet()
        }
        insertLetterDeck(name = title)

        val practiceId = getLastInsertRowId().executeAsOne()
        characters.forEach { character ->
            if (character !in charactersToLeave) insertOrIgnoreLetterDeckEntry(character, practiceId)
        }
    }

    override suspend fun createDeckAndMerge(
        title: String,
        deckIdToMerge: List<Long>,
        conflictResolutions: Map<String, LetterDeckConflictResolution>,
    ): List<LetterDeckConflict> = writeDeckWithConflicts {
        val charactersToLeave = if (onlyAllowSingleDeckPerKanji()) {
            val characters = deckIdToMerge.chunked(QUERY_BATCH_SIZE).flatMap { deckIds ->
                getCharactersForLetterDecks(deckIds).executeAsList()
            }
            resolveDeckConflicts(characters, conflictResolutions, mergedDeckIds = deckIdToMerge.toSet())
        } else {
            emptySet()
        }

        insertLetterDeck(name = title)
        val deckId = getLastInsertRowId().executeAsOne()

        migrateLetterDeckEntries(deckId, deckIdToMerge)
        charactersToLeave.forEach { deleteLetterDeckEntry(deckId, it) }
        migrateDeckForReviewHistory(
            deckId = deckId,
            deckIdToMigrate = deckIdToMerge,
            practiceTypes = LetterPracticeType.srsPracticeTypeValues
        )

        deleteLetterDecks(deckIdToMerge)
    }

    override suspend fun updateDeckPositions(
        deckIdToPositionMap: Map<Long, Int>,
    ) = writeTransaction {
        deckIdToPositionMap.forEach { (practiceId, position) ->
            updateLetterDeckPosition(position.toLong(), practiceId)
        }
    }

    override suspend fun deleteDeck(id: Long) = writeTransaction { deleteLetterDeck(id) }

    override suspend fun updateDeck(
        id: Long,
        title: String,
        charactersToAdd: List<String>,
        charactersToRemove: List<String>,
        conflictResolutions: Map<String, LetterDeckConflictResolution>,
    ): List<LetterDeckConflict> = writeDeckWithConflicts {
        val charactersToLeave = if (onlyAllowSingleDeckPerKanji()) {
            // Ensure the destination still exists before removing anything from other decks.
            getLetterDeck(id).executeAsOne()
            resolveDeckConflicts(
                charactersToAdd - charactersToRemove.toSet(),
                conflictResolutions,
                targetDeckId = id
            )
        } else {
            emptySet()
        }
        updateLetterDeckTitle(title, id)
        charactersToAdd.forEach { character ->
            if (character !in charactersToLeave) insertOrIgnoreLetterDeckEntry(character, id)
        }
        charactersToRemove.forEach { deleteLetterDeckEntry(id, it) }
    }

    override suspend fun getDecks(): List<LetterDeck> = readTransaction {
        getAllLetterDecks().executeAsList().map {
            LetterDeck(it.id, it.name, it.position.toInt())
        }
    }

    override suspend fun getDeck(
        id: Long,
    ): LetterDeck = readTransaction {
        getLetterDeck(id).executeAsOne().run { LetterDeck(id, name, position.toInt()) }
    }

    override suspend fun getDeckCharacters(
        id: Long,
    ): List<String> = readTransaction {
        getEntriesForLetterDeck(id).executeAsList().map { it.character }
    }

    override suspend fun getDeckConflicts(
        characters: List<String>,
        targetDeckId: Long?,
    ): List<LetterDeckConflict> = readTransaction {
        if (onlyAllowSingleDeckPerKanji()) findDeckConflicts(characters.kanji(), targetDeckId)
        else emptyList()
    }

    private fun UserDataQueries.onlyAllowSingleDeckPerKanji(): Boolean =
        getOnlyAllowSingleDeckPerKanji().executeAsOneOrNull() == 1L

    private suspend fun writeDeckWithConflicts(
        block: UserDataQueries.() -> Unit,
    ): List<LetterDeckConflict> = try {
        writeTransaction(block)
        emptyList()
    } catch (exception: UnresolvedDeckConflicts) {
        exception.conflicts
    }

    private fun UserDataQueries.resolveDeckConflicts(
        characters: List<String>,
        resolutions: Map<String, LetterDeckConflictResolution>,
        targetDeckId: Long? = null,
        mergedDeckIds: Set<Long> = emptySet(),
    ): Set<String> {
        val kanji = characters.kanji()
        val conflicts = findDeckConflicts(kanji, targetDeckId, mergedDeckIds)
        val unresolved = conflicts.filter { conflict ->
            when (val resolution = resolutions[conflict.character]) {
                null -> true
                LetterDeckConflictResolution.Leave -> false
                is LetterDeckConflictResolution.Move ->
                    conflict.decks.any { it.id !in resolution.sourceDeckIds }
            }
        }
        if (unresolved.isNotEmpty()) throw UnresolvedDeckConflicts(unresolved)

        conflicts.forEach { conflict ->
            if (resolutions[conflict.character] is LetterDeckConflictResolution.Move) {
                conflict.decks.forEach { deleteLetterDeckEntry(it.id, conflict.character) }
            }
        }
        return kanji.filter { resolutions[it] == LetterDeckConflictResolution.Leave }.toSet()
    }

    private fun UserDataQueries.findDeckConflicts(
        kanji: List<String>,
        targetDeckId: Long?,
        mergedDeckIds: Set<Long> = emptySet(),
    ): List<LetterDeckConflict> {
        val decksByKanji = kanji.chunked(QUERY_BATCH_SIZE).flatMap { batch ->
            getLetterDecksForCharacters(batch).executeAsList()
        }.groupBy(
            keySelector = { it.character },
            valueTransform = { LetterDeck(it.id, it.name, it.position.toInt()) }
        )

        return kanji.mapNotNull { character ->
            val decks = decksByKanji[character].orEmpty()
            if (decks.any { it.id == targetDeckId }) return@mapNotNull null

            val sourceDecks = decks.filterNot { it.id in mergedDeckIds }
            if (sourceDecks.isEmpty()) null else LetterDeckConflict(character, sourceDecks)
        }
    }

    private fun List<String>.kanji(): List<String> = distinct().filter { character ->
        when (character.length) {
            1 -> when (character[0]) {
                in '\u3400'..'\u4DBF', in '\u4E00'..'\u9FFF', in '\uF900'..'\uFAFF', '〇' -> true
                else -> false
            }
            2 -> {
                val high = character[0]
                val low = character[1]
                if (high !in '\uD800'..'\uDBFF' || low !in '\uDC00'..'\uDFFF') return@filter false
                val codePoint = ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
                codePoint in 0x20000..0x2FA1F || codePoint in 0x30000..0x323AF
            }
            else -> false
        }
    }

    private class UnresolvedDeckConflicts(val conflicts: List<LetterDeckConflict>) : RuntimeException()

    private companion object {
        // Stay below the SQLite bind parameter limit on older Android versions.
        const val QUERY_BATCH_SIZE = 500
    }

}

