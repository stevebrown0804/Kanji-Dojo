package ua.syt0r.kanji.core.user_data.database.sqldelight

import ua.syt0r.kanji.core.srs.MojiPracticeType
import ua.syt0r.kanji.core.user_data.database.MojiDeck
import ua.syt0r.kanji.core.user_data.database.MojiDeckConflict
import ua.syt0r.kanji.core.user_data.database.MojiDeckConflictResolution
import ua.syt0r.kanji.core.user_data.database.MojiPracticeRepository
import ua.syt0r.kanji.core.user_data.database.ObservableUserDataRepository
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.userdata.db.UserDataQueries

class SqlDelightMojiPracticeRepository(
    databaseManager: UserDataDatabaseContract.Manager,
) : ObservableUserDataRepository(databaseManager), MojiPracticeRepository {

    override suspend fun createDeck(
        title: String,
        characters: List<String>,
        conflictResolutions: Map<String, MojiDeckConflictResolution>,
    ): List<MojiDeckConflict> = writeDeckWithConflicts {
        val charactersToLeave = if (onlyAllowSingleDeckPerKanji()) {
            resolveDeckConflicts(characters, conflictResolutions)
        } else {
            emptySet()
        }
        insertMojiDeck(name = title)

        val practiceId = getLastInsertRowId().executeAsOne()
        characters.forEach { character ->
            if (character !in charactersToLeave) insertOrIgnoreMojiDeckEntry(character, practiceId)
        }
    }

    override suspend fun createDeckAndMerge(
        title: String,
        deckIdToMerge: List<Long>,
        conflictResolutions: Map<String, MojiDeckConflictResolution>,
    ): List<MojiDeckConflict> = writeDeckWithConflicts {
        val charactersToLeave = if (onlyAllowSingleDeckPerKanji()) {
            val characters = deckIdToMerge.chunked(QUERY_BATCH_SIZE).flatMap { deckIds ->
                getCharactersForMojiDecks(deckIds).executeAsList()
            }
            resolveDeckConflicts(characters, conflictResolutions, mergedDeckIds = deckIdToMerge.toSet())
        } else {
            emptySet()
        }

        insertMojiDeck(name = title)
        val deckId = getLastInsertRowId().executeAsOne()

        migrateMojiDeckEntries(deckId, deckIdToMerge)
        charactersToLeave.forEach { deleteMojiDeckEntry(deckId, it) }
        migrateDeckForReviewHistory(
            deckId = deckId,
            deckIdToMigrate = deckIdToMerge,
            practiceTypes = MojiPracticeType.srsPracticeTypeValues
        )

        deleteMojiDecks(deckIdToMerge)
    }

    override suspend fun updateDeckPositions(
        deckIdToPositionMap: Map<Long, Int>,
    ) = writeTransaction {
        deckIdToPositionMap.forEach { (practiceId, position) ->
            updateMojiDeckPosition(position.toLong(), practiceId)
        }
    }

    override suspend fun deleteDeck(id: Long) = writeTransaction { deleteMojiDeck(id) }

    override suspend fun updateDeck(
        id: Long,
        title: String,
        charactersToAdd: List<String>,
        charactersToRemove: List<String>,
        conflictResolutions: Map<String, MojiDeckConflictResolution>,
    ): List<MojiDeckConflict> = writeDeckWithConflicts {
        val charactersToLeave = if (onlyAllowSingleDeckPerKanji()) {
            // Ensure the destination still exists before removing anything from other decks.
            getMojiDeck(id).executeAsOne()
            resolveDeckConflicts(
                charactersToAdd - charactersToRemove.toSet(),
                conflictResolutions,
                targetDeckId = id
            )
        } else {
            emptySet()
        }
        updateMojiDeckTitle(title, id)
        charactersToAdd.forEach { character ->
            if (character !in charactersToLeave) insertOrIgnoreMojiDeckEntry(character, id)
        }
        charactersToRemove.forEach { deleteMojiDeckEntry(id, it) }
    }

    override suspend fun tryRemoveMojiFromDeck(
        character: String,
        deckId: Long,
        allowLastDeckRemoval: Boolean,
    ): Boolean = try {
        writeTransaction {
            val containingDecks = getMojiDecksForCharacters(listOf(character)).executeAsList()
            if (containingDecks.none { it.id == deckId }) {
                throw MojiDeckRemovalSkipped(alreadyAbsent = true)
            }
            if (containingDecks.size == 1 && !allowLastDeckRemoval) {
                throw MojiDeckRemovalSkipped(alreadyAbsent = false)
            }
            deleteMojiDeckEntry(deckId, character)
        }
        true
    } catch (exception: MojiDeckRemovalSkipped) {
        exception.alreadyAbsent
    }

    override suspend fun getDecks(): List<MojiDeck> = readTransaction {
        getAllMojiDecks().executeAsList().map {
            MojiDeck(it.id, it.name, it.position.toInt())
        }
    }

    override suspend fun getDecksContainingMoji(character: String): List<MojiDeck> = readTransaction {
        getMojiDecksForCharacters(listOf(character)).executeAsList().map {
            MojiDeck(it.id, it.name, it.position.toInt())
        }
    }

    override suspend fun getDeck(
        id: Long,
    ): MojiDeck = readTransaction {
        getMojiDeck(id).executeAsOne().run { MojiDeck(id, name, position.toInt()) }
    }

    override suspend fun getDeckCharacters(
        id: Long,
    ): List<String> = readTransaction {
        getEntriesForMojiDeck(id).executeAsList().map { it.character }
    }

    override suspend fun getDeckConflicts(
        characters: List<String>,
        targetDeckId: Long?,
    ): List<MojiDeckConflict> = readTransaction {
        if (onlyAllowSingleDeckPerKanji()) findDeckConflicts(characters.kanji(), targetDeckId)
        else emptyList()
    }

    private fun UserDataQueries.onlyAllowSingleDeckPerKanji(): Boolean =
        getOnlyAllowSingleDeckPerKanji().executeAsOneOrNull() == 1L

    private suspend fun writeDeckWithConflicts(
        block: UserDataQueries.() -> Unit,
    ): List<MojiDeckConflict> = try {
        writeTransaction(block)
        emptyList()
    } catch (exception: UnresolvedDeckConflicts) {
        exception.conflicts
    }

    private fun UserDataQueries.resolveDeckConflicts(
        characters: List<String>,
        resolutions: Map<String, MojiDeckConflictResolution>,
        targetDeckId: Long? = null,
        mergedDeckIds: Set<Long> = emptySet(),
    ): Set<String> {
        val kanji = characters.kanji()
        val conflicts = findDeckConflicts(kanji, targetDeckId, mergedDeckIds)
        val unresolved = conflicts.filter { conflict ->
            when (val resolution = resolutions[conflict.character]) {
                null -> true
                MojiDeckConflictResolution.Leave -> false
                is MojiDeckConflictResolution.Move ->
                    conflict.decks.any { it.id !in resolution.sourceDeckIds }
            }
        }
        if (unresolved.isNotEmpty()) throw UnresolvedDeckConflicts(unresolved)

        conflicts.forEach { conflict ->
            if (resolutions[conflict.character] is MojiDeckConflictResolution.Move) {
                conflict.decks.forEach { deleteMojiDeckEntry(it.id, conflict.character) }
            }
        }
        return kanji.filter { resolutions[it] == MojiDeckConflictResolution.Leave }.toSet()
    }

    private fun UserDataQueries.findDeckConflicts(
        kanji: List<String>,
        targetDeckId: Long?,
        mergedDeckIds: Set<Long> = emptySet(),
    ): List<MojiDeckConflict> {
        val decksByKanji = kanji.chunked(QUERY_BATCH_SIZE).flatMap { batch ->
            getMojiDecksForCharacters(batch).executeAsList()
        }.groupBy(
            keySelector = { it.character },
            valueTransform = { MojiDeck(it.id, it.name, it.position.toInt()) }
        )

        return kanji.mapNotNull { character ->
            val decks = decksByKanji[character].orEmpty()
            if (decks.any { it.id == targetDeckId }) return@mapNotNull null

            val sourceDecks = decks.filterNot { it.id in mergedDeckIds }
            if (sourceDecks.isEmpty()) null else MojiDeckConflict(character, sourceDecks)
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

    private class UnresolvedDeckConflicts(val conflicts: List<MojiDeckConflict>) : RuntimeException()

    private class MojiDeckRemovalSkipped(val alreadyAbsent: Boolean) : RuntimeException()

    private companion object {
        // Stay below the SQLite bind parameter limit on older Android versions.
        const val QUERY_BATCH_SIZE = 500
    }

}

