import androidx.compose.ui.graphics.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ua.syt0r.kanji.core.stroke_evaluator.KanjiStrokeEvaluator
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterInputData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriterConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWriterContent
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.CharacterWritingProgress
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.DefaultCharacterWriterState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.StrokeProcessingResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CharacterWriterRestartTest {

    @Test
    fun restartClearsAcceptedStrokesAndAllMistakesBeforeRedrawing() = withWriter { writer, scope ->
        writer.submitStroke()
        scope.awaitProcessing()
        val original = assertIs<CharacterWriterContent.SingleStrokeInput>(writer.content.value)
        assertEquals(1, original.drawnStrokesCount.value)
        original.notifyHintClick()
        assertEquals(1, original.currentStrokeMistakes.value)
        assertEquals(1, original.totalMistakes.value)

        writer.restartDrawing()

        val restarted = assertIs<CharacterWriterContent.SingleStrokeInput>(writer.content.value)
        assertNotSame(original, restarted)
        assertEquals(0, restarted.drawnStrokesCount.value)
        assertEquals(0, restarted.currentStrokeMistakes.value)
        assertEquals(0, restarted.totalMistakes.value)
        assertEquals(CharacterWritingProgress.Writing, writer.progress.value)

        original.notifyHintClick()
        assertEquals(0, restarted.totalMistakes.value)
        assertEquals(0, restarted.currentStrokeMistakes.value)

        repeat(writer.strokes.size) {
            writer.submitStroke()
            scope.awaitProcessing()
        }
        val completed = assertIs<CharacterWritingProgress.Completed>(writer.progress.value)
        assertEquals(0, completed.mistakes)
        assertTrue(completed.isCorrect)
    }

    @Test
    fun restartLeavesCompletedAndWholeCharacterInputUnchanged() {
        withWriter(strokeCount = 1) { writer, scope ->
            writer.submitStroke()
            scope.awaitProcessing()
            val completedContent = writer.content.value
            assertIs<CharacterWritingProgress.Completed>(writer.progress.value)

            writer.restartDrawing()

            assertSame(completedContent, writer.content.value)
            assertIs<CharacterWritingProgress.Completed>(writer.progress.value)
        }

        withWriter(configuration = CharacterWriterConfiguration.CharacterInput) { writer, _ ->
            val input = assertIs<CharacterWriterContent.MultipleStrokeInput.Writing>(writer.content.value)
            input.strokes.value = listOf(Path())

            writer.restartDrawing()

            assertSame(input, writer.content.value)
            assertEquals(1, input.strokes.value.size)
        }
    }

    @Test
    fun finishingEvaluationsCannotChangeRestartedInput() {
        listOf(true, false).forEach { evaluatorResult ->
            val evaluationStarted = CountDownLatch(1)
            val releaseEvaluation = CountDownLatch(1)
            val evaluator = object : KanjiStrokeEvaluator {
                override fun areStrokesSimilar(first: Path, second: Path): Boolean {
                    evaluationStarted.countDown()
                    check(releaseEvaluation.await(5, TimeUnit.SECONDS))
                    return evaluatorResult
                }
            }

            withWriter(evaluator = evaluator) { writer, scope ->
                try {
                    val original = assertIs<CharacterWriterContent.SingleStrokeInput>(writer.content.value)
                    original.notifyHintClick()
                    writer.submitStroke()
                    assertTrue(evaluationStarted.await(5, TimeUnit.SECONDS))
                    val processingJobs = scope.coroutineContext[Job]!!.children.toList()

                    writer.restartDrawing()
                    val restarted = assertIs<CharacterWriterContent.SingleStrokeInput>(writer.content.value)
                    val receivedResults = mutableListOf<StrokeProcessingResult>()
                    val collector = scope.launch {
                        restarted.inputProcessingResults.collect { receivedResults.add(it) }
                    }
                    try {
                        releaseEvaluation.countDown()
                        withTimeout(5_000) { processingJobs.joinAll() }

                        assertSame(restarted, writer.content.value)
                        assertEquals(0, restarted.drawnStrokesCount.value)
                        assertEquals(0, restarted.currentStrokeMistakes.value)
                        assertEquals(0, restarted.totalMistakes.value)
                        assertEquals(CharacterWritingProgress.Writing, writer.progress.value)
                        assertTrue(receivedResults.isEmpty())
                    } finally {
                        collector.cancelAndJoin()
                    }
                } finally {
                    releaseEvaluation.countDown()
                }
            }
        }
    }

    private fun withWriter(
        strokeCount: Int = 3,
        configuration: CharacterWriterConfiguration = CharacterWriterConfiguration.StrokeInput(isStudyMode = false),
        evaluator: KanjiStrokeEvaluator = AlwaysCorrectEvaluator,
        block: suspend (DefaultCharacterWriterState, CoroutineScope) -> Unit
    ) = runBlocking {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Unconfined)
        val writer = DefaultCharacterWriterState(
            coroutineScope = scope,
            strokeEvaluator = evaluator,
            character = "日",
            strokes = List(strokeCount) { Path() },
            configuration = configuration
        )
        try {
            block(writer, scope)
        } finally {
            job.cancelAndJoin()
        }
    }

    private fun DefaultCharacterWriterState.submitStroke() {
        val input = assertIs<CharacterWriterContent.SingleStrokeInput>(content.value)
        submit(CharacterInputData.SingleStroke(userPath = Path(), kanjiPath = strokes[input.drawnStrokesCount.value]))
    }

    private suspend fun CoroutineScope.awaitProcessing() {
        val processingJobs = coroutineContext[Job]!!.children.toList()
        withTimeout(5_000) { processingJobs.joinAll() }
    }

    private object AlwaysCorrectEvaluator : KanjiStrokeEvaluator {
        override fun areStrokesSimilar(first: Path, second: Path): Boolean = true
    }
}
