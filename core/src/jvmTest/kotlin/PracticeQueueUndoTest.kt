import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.analytics.PrintAnalyticsManager
import ua.syt0r.kanji.core.srs.DefaultSrsCardRepository
import ua.syt0r.kanji.core.srs.DefaultSrsScheduler
import ua.syt0r.kanji.core.srs.MojiPracticeType
import ua.syt0r.kanji.core.srs.fsrs.DefaultFsrsScheduler
import ua.syt0r.kanji.core.srs.fsrs.Fsrs5
import ua.syt0r.kanji.core.time.DefaultTimeUtils
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightFsrsCardRepository
import ua.syt0r.kanji.core.user_data.database.sqldelight.SqlDelightReviewHistoryRepository
import ua.syt0r.kanji.presentation.common.Paginateable
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeQueueProgress
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.DefaultMojiPracticeQueue
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeExampleWord
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeItemData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeLayoutConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueItemDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.data.MojiPracticeQueueState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_moji.use_case.GetMojiPracticeQueueItemDataUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PracticeQueueUndoTest {

    @Test
    fun multipleUndosRestoreCardOrderAndAllCounters() = withQueue("休", "木") { queue, history ->
        val first = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        assertFalse(first.canUndo)
        assertEquals(0L, first.answers.again.srsAnswer.card.interval.inWholeDays)

        queue.submitAnswer(first.answers.again)
        val afterRepeat = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        assertEquals("木", afterRepeat.descriptor.character)
        assertEquals(PracticeQueueProgress(pending = 1, repeats = 1, completed = 0), afterRepeat.progress)
        assertTrue(afterRepeat.canUndo)

        queue.submitAnswer(afterRepeat.answers.easy)
        val afterCompletion = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        assertEquals("休", afterCompletion.descriptor.character)
        assertEquals(1, afterCompletion.currentItemRepeat)
        assertEquals(PracticeQueueProgress(pending = 0, repeats = 1, completed = 1), afterCompletion.progress)

        assertTrue(queue.undoLastReview())
        val restoredSecond = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        assertEquals(afterRepeat.descriptor, restoredSecond.descriptor)
        assertEquals(afterRepeat.progress, restoredSecond.progress)
        assertTrue(restoredSecond.canUndo)

        assertTrue(queue.undoLastReview())
        val restoredFirst = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        assertEquals(first.descriptor, restoredFirst.descriptor)
        assertEquals(first.progress, restoredFirst.progress)
        assertEquals(first.answers, restoredFirst.answers)
        assertEquals(2, restoredFirst.undoCount)
        assertFalse(restoredFirst.canUndo)
        assertFalse(queue.undoLastReview())
        assertEquals(0L, history.getTotalReviewsCount())

        queue.submitAnswer(restoredFirst.answers.easy)
        val next = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        assertEquals("木", next.descriptor.character)
        assertEquals(PracticeQueueProgress(pending = 1, repeats = 0, completed = 1), next.progress)
    }

    @Test
    fun undoFromNaturalSummaryRestoresTheCardAndItsPriorSummary() = withQueue("休") { queue, history ->
        val initial = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        queue.submitAnswer(initial.answers.again)
        val repeated = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        assertEquals(1, repeated.currentItemRepeat)

        queue.submitAnswer(repeated.answers.easy)
        val finished = assertIs<MojiPracticeQueueState.Summary>(queue.state.value)
        assertTrue(finished.canUndo)
        assertEquals(2, finished.items.single().totalReviews.await())

        assertTrue(queue.undoLastReview())
        val restored = assertIs<MojiPracticeQueueState.Review>(queue.state.value)
        assertEquals(repeated.descriptor, restored.descriptor)
        assertEquals(repeated.currentItemRepeat, restored.currentItemRepeat)
        assertEquals(repeated.progress, restored.progress)
        assertTrue(restored.canUndo)

        queue.immediateFinish()
        val endedEarly = assertIs<MojiPracticeQueueState.Summary>(queue.state.value)
        assertFalse(endedEarly.canUndo)
        assertFalse(queue.undoLastReview())
        assertEquals(1, endedEarly.items.single().totalReviews.await())
        assertEquals(initial.answers.again.srsAnswer.card.interval, endedEarly.items.single().nextInterval)
        val key = MojiPracticeType.Reading.toSrsKey("休")
        assertEquals(1L, history.getTotalReviewCount(key.itemKey, key.practiceType))
    }

    private fun withQueue(
        vararg characters: String,
        block: suspend (DefaultMojiPracticeQueue, SqlDelightReviewHistoryRepository) -> Unit
    ) = withDatabase { manager, _ ->
        val job = SupervisorJob()
        val scope = CoroutineScope(job)
        val history = SqlDelightReviewHistoryRepository(manager)
        try {
            val queue = DefaultMojiPracticeQueue(
                coroutineScope = scope,
                timeUtils = object : TimeUtils by DefaultTimeUtils {
                    override fun now() = Instant.parse("2026-11-14T00:00:00Z")
                },
                srsCardRepository = DefaultSrsCardRepository(SqlDelightFsrsCardRepository(manager)),
                reviewHistoryRepository = history,
                srsScheduler = DefaultSrsScheduler(DefaultFsrsScheduler(Fsrs5())),
                getQueueItemDataUseCase = TestReadingData,
                analyticsManager = PrintAnalyticsManager()
            )
            val layout = MojiPracticeLayoutConfiguration.ReadingLayoutConfiguration(
                kanaAutoPlay = mutableStateOf(false)
            )
            queue.initialize(characters.map { character ->
                MojiPracticeQueueItemDescriptor.Reading(
                    character = character,
                    deckId = 1,
                    romajiReading = false,
                    layoutConfiguration = layout
                )
            })
            block(queue, history)
        } finally {
            job.cancelAndJoin()
        }
    }

    private object TestReadingData : GetMojiPracticeQueueItemDataUseCase {
        override suspend fun invoke(
            descriptor: MojiPracticeQueueItemDescriptor,
            coroutineScope: CoroutineScope
        ): MojiPracticeItemData = MojiPracticeItemData.KanjiReadingData(
            character = descriptor.character,
            examples = object : Paginateable<MojiPracticeExampleWord> {
                override val total = 0
                override val list = MutableStateFlow(emptyList<MojiPracticeExampleWord>())
                override val canLoadMore = MutableStateFlow(false)
                override fun loadMore() = Unit
            },
            radicals = emptyList(),
            on = emptyList(),
            kun = emptyList(),
            meanings = emptyList(),
            variants = null
        )
    }
}
