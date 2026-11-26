package ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data

import kotlinx.coroutines.Deferred
import ua.syt0r.kanji.core.srs.MojiPracticeType
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.stroke_evaluator.KanjiStrokeEvaluator
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswers
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeQueueItem
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeQueueProgress
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeSummaryItem
import kotlin.time.Duration


sealed interface MojiPracticeQueueState {

    object Loading : MojiPracticeQueueState

    data class Review(
        val descriptor: MojiPracticeQueueItemDescriptor,
        val data: MojiPracticeItemData,
        val currentItemRepeat: Int,
        val progress: PracticeQueueProgress,
        val answers: PracticeAnswers,
        val canUndo: Boolean,
        val undoCount: Int
    ) : MojiPracticeQueueState

    data class Summary(
        val duration: Duration,
        val items: List<MojiPracticeSummaryItem>,
        val canUndo: Boolean
    ) : MojiPracticeQueueState

}

sealed interface MojiPracticeQueueItemDescriptor {

    val character: String
    val practiceType: MojiPracticeType
    val deckId: Long
    val romajiReading: Boolean
    val layoutConfiguration: MojiPracticeLayoutConfiguration

    data class Writing(
        override val character: String,
        override val deckId: Long,
        override val romajiReading: Boolean,
        override val layoutConfiguration: MojiPracticeLayoutConfiguration.WritingLayoutConfiguration,
        val inputMode: WritingPracticeInputMode,
        val evaluator: KanjiStrokeEvaluator,
        val shouldStudy: Boolean
    ) : MojiPracticeQueueItemDescriptor {
        override val practiceType = MojiPracticeType.Writing
    }

    data class Reading(
        override val character: String,
        override val deckId: Long,
        override val romajiReading: Boolean,
        override val layoutConfiguration: MojiPracticeLayoutConfiguration.ReadingLayoutConfiguration
    ) : MojiPracticeQueueItemDescriptor {
        override val practiceType: MojiPracticeType = MojiPracticeType.Reading
    }

}

data class MojiPracticeQueueItem(
    val descriptor: MojiPracticeQueueItemDescriptor,
    override val srsCardKey: SrsCardKey,
    override val srsCard: SrsCard,
    override val deckId: Long,
    override val repeats: Int,
    override val totalMistakes: Int,
    override val data: Deferred<MojiPracticeItemData>
) : PracticeQueueItem<MojiPracticeQueueItem> {

    override fun copyForRepeat(answer: PracticeAnswer): MojiPracticeQueueItem {
        return copy(
            srsCard = answer.srsAnswer.card,
            repeats = repeats + 1,
            totalMistakes = totalMistakes + answer.mistakes
        )
    }

}

sealed interface MojiPracticeSummaryItem : PracticeSummaryItem {

    val moji: String
    override val nextInterval: Duration

    data class Writing(
        override val moji: String,
        override val nextInterval: Duration,
        override val totalReviews: Deferred<Int>,
        val strokeCount: Int,
        val mistakes: Int
    ) : MojiPracticeSummaryItem

    data class Reading(
        override val moji: String,
        override val totalReviews: Deferred<Int>,
        override val nextInterval: Duration
    ) : MojiPracticeSummaryItem

}
