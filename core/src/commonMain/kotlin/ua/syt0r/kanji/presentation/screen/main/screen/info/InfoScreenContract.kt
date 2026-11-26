package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.State
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import kotlinx.coroutines.CoroutineScope
import ua.syt0r.kanji.core.user_data.database.MojiDeck

interface InfoScreenContract {

    companion object {

        const val ListPageItemsCount: Int = 50
        const val ListPrefetchDistance: Int = 50

        fun getJishoSearchUrl(searchTerm: String): String {
            val url = buildUrl {
                protocol = URLProtocol.HTTPS
                host = "jisho.org"
                appendPathSegments("search", searchTerm)
            }
            return url.toString()
        }

    }

    interface ViewModel {
        val state: State<ScreenState>
        val deckRemovalPrompt: State<KanjiDeckRemovalPrompt?>
        fun requestDeckRemoval(deck: MojiDeck)
        fun confirmDeckRemoval(prompt: KanjiDeckRemovalPrompt)
        fun cancelDeckRemoval(prompt: KanjiDeckRemovalPrompt)
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class NoData(
            val data: InfoScreenData
        ) : ScreenState

        sealed interface Loaded : ScreenState {

            data class Moji(
                val data: MojiInfoData
            ) : Loaded

            data class Vocab(
                val data: VocabInfoData
            ) : Loaded

        }

    }

    interface LoadMojiStateUseCase {
        suspend operator fun invoke(character: String, coroutineScope: CoroutineScope): ScreenState
    }

    interface LoadVocabStateUseCase {
        suspend operator fun invoke(
            data: InfoScreenData.Vocab,
            coroutineScope: CoroutineScope
        ): ScreenState
    }

}
