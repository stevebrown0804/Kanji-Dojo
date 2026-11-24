package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case

import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.japanese.isKana

interface ValidateDeckCharactersUseCase {
    suspend operator fun invoke(input: String): AddCharactersResult
    suspend operator fun invoke(character: List<String>): AddCharactersResult
}

class AddCharactersResult(
    val detectedCharacter: List<String>,
    val unknownCharacters: List<String>,
    val readingsByCharacter: Map<String, List<String>> = emptyMap()
)


class DefaultValidateDeckCharactersUseCase(
    private val appDataRepository: AppDataRepository
) : ValidateDeckCharactersUseCase {

    override suspend fun invoke(input: String): AddCharactersResult {
        return processInput(input = input.map { it })
    }

    override suspend fun invoke(character: List<String>): AddCharactersResult {
        return processInput(input = character.map { it.first() })
    }

    private suspend fun processInput(input: Iterable<Char>): AddCharactersResult {
        val parsedCharacters = input.distinct()

        val known = mutableListOf<String>()
        val unknown = mutableListOf<String>()
        val readingsByCharacter = mutableMapOf<String, List<String>>()

        parsedCharacters.forEach { character ->
            val characterText = character.toString()
            val hasStrokes = appDataRepository.getStrokes(characterText).isNotEmpty()

            when {
                !hasStrokes -> unknown.add(characterText)
                character.isKana() -> known.add(characterText)
                else -> {
                    val readings = appDataRepository.getReadings(characterText).keys
                    if (readings.isEmpty()) {
                        unknown.add(characterText)
                    } else {
                        known.add(characterText)
                        readingsByCharacter[characterText] = readings.toList()
                    }
                }
            }
        }

        return AddCharactersResult(
            detectedCharacter = known,
            unknownCharacters = unknown,
            readingsByCharacter = readingsByCharacter
        )
    }

}
