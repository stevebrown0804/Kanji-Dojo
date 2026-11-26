package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case

import kotlinx.coroutines.CoroutineScope
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriterConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.DefaultCharacterWriterState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeItemData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueItemDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeReviewState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.WritingPracticeInputMode

interface GetMojiPracticeReviewStateUseCase {
    operator fun invoke(queueState: MojiPracticeQueueState.Review): MojiPracticeReviewState
}

class DefaultGetMojiPracticeReviewStateUseCase(
    private val characterWriterCoroutineScope: CoroutineScope
) : GetMojiPracticeReviewStateUseCase {

    override fun invoke(queueState: MojiPracticeQueueState.Review): MojiPracticeReviewState {
        return when (val descriptor = queueState.descriptor) {
            is MojiPracticeQueueItemDescriptor.Writing -> {
                val data = queueState.data as MojiPracticeItemData.WritingData
                MojiPracticeReviewState.Writing(
                    layout = descriptor.layoutConfiguration,
                    itemData = data,
                    answers = queueState.answers,
                    studyWriterState = when (
                        descriptor.shouldStudy && queueState.currentItemRepeat == 0
                    ) {
                        true -> DefaultCharacterWriterState(
                            coroutineScope = characterWriterCoroutineScope,
                            strokeEvaluator = descriptor.evaluator,
                            character = descriptor.character,
                            strokes = data.strokes,
                            configuration = CharacterWriterConfiguration.StrokeInput(isStudyMode = true)
                        )

                        false -> null
                    },
                    reviewWriterState = DefaultCharacterWriterState(
                        coroutineScope = characterWriterCoroutineScope,
                        strokeEvaluator = descriptor.evaluator,
                        character = descriptor.character,
                        strokes = data.strokes,
                        configuration = when (descriptor.inputMode) {
                            WritingPracticeInputMode.Stroke -> {
                                CharacterWriterConfiguration.StrokeInput(isStudyMode = false)
                            }

                            WritingPracticeInputMode.Character -> CharacterWriterConfiguration.CharacterInput
                        }
                    )
                )
            }

            is MojiPracticeQueueItemDescriptor.Reading -> {
                MojiPracticeReviewState.Reading(
                    layout = descriptor.layoutConfiguration,
                    itemData = queueState.data as MojiPracticeItemData.ReadingData,
                    answers = queueState.answers
                )
            }
        }
    }
}