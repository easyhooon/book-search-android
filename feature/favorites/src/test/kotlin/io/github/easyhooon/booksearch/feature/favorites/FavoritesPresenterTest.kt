package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.FavoriteSort
import io.github.easyhooon.booksearch.core.model.PriceFilter
import io.github.easyhooon.booksearch.core.testing.FakeFavoriteWriter
import io.github.easyhooon.booksearch.core.testing.MainDispatcherRule
import io.github.easyhooon.booksearch.core.testing.awaitItemMatching
import io.github.easyhooon.booksearch.core.testing.presenterFlow
import io.github.easyhooon.booksearch.core.testing.testBook
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import java.io.IOException

/** Android용 Compose runtime이 Trace·Log를 호출하므로 Robolectric에서 실행합니다. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSoilQueryApi::class)
class FavoritesPresenterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val writer = FakeFavoriteWriter()

    /** Root가 Room Subscription에서 읽어 넘기는 값입니다. null은 아직 받지 못한 상태입니다. */
    private var favoriteBooks by mutableStateOf<List<Book>?>(null)
    private lateinit var latest: FavoritesUiState

    private val kotlin = testBook("k", title = "코틀린 인 액션", authors = listOf("Dmitry Jemerov"), price = 36_000)
    private val android = testBook("a", title = "안드로이드 프로그래밍", authors = listOf("홍길동"), price = 30_000)
    private val git = testBook("g", title = "깃 교과서", authors = listOf("이고잉"), price = null)
    private val compose = testBook("c", title = "가나다 컴포즈", authors = listOf("김작가"), price = 0)

    private fun runPresenterTest(validate: suspend ReceiveTurbine<FavoritesUiState>.() -> Unit) = runTest {
        val client = SwrCachePlus(backgroundScope)
        presenterFlow(client) {
            favoritesPresenter(favoriteBooks = favoriteBooks, favoriteMutationKey = writer.key)
        }.onEach { latest = it }.test {
            validate()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 가장 최근 상태의 eventSink로 보냅니다. 방출 대기열은 소비하지 않습니다. */
    private fun send(event: FavoritesEvent) = latest.eventSink(event)

    private suspend fun ReceiveTurbine<FavoritesUiState>.awaitIds(ids: List<String>): FavoritesUiState =
        awaitItemMatching { (it.content as? FavoritesContent.Results)?.books?.map(Book::id) == ids }

    @Test
    fun `Room 값을 받기 전에는 로딩이고 비어 있으면 즐겨찾기 없음이다`() = runPresenterTest {
        assertEquals(FavoritesContent.Loading, awaitItem().content)

        favoriteBooks = emptyList()
        awaitItemMatching { it.content == FavoritesContent.NoFavorites }
    }

    @Test
    fun `기본은 한국어 제목 오름차순이고 내림차순으로 바꿀 수 있다`() {
        favoriteBooks = listOf(kotlin, android, git, compose)
        runPresenterTest {
            val initial = awaitIds(listOf("c", "g", "a", "k"))
            assertEquals(FavoriteSort.TitleAscending, initial.sort)

            send(FavoritesEvent.SortSelected(FavoriteSort.TitleDescending))
            awaitIds(listOf("k", "a", "g", "c"))
        }
    }

    @Test
    fun `검색어는 확정해야 적용되고 제목이나 저자에 대소문자 없이 부분 일치한다`() {
        favoriteBooks = listOf(kotlin, android, git, compose)
        runPresenterTest {
            awaitIds(listOf("c", "g", "a", "k"))

            send(FavoritesEvent.InputChanged("jemerov"))
            val typed = awaitItemMatching { it.input == "jemerov" }
            assertEquals(4, (typed.content as FavoritesContent.Results).books.size)

            send(FavoritesEvent.Submit)
            awaitIds(listOf("k"))

            send(FavoritesEvent.InputChanged(" 프로그래밍 "))
            send(FavoritesEvent.Submit)
            awaitIds(listOf("a"))
        }
    }

    @Test
    fun `공백만 확정하면 전체를 보여 준다`() {
        favoriteBooks = listOf(kotlin, android)
        runPresenterTest {
            send(FavoritesEvent.InputChanged("코틀린"))
            send(FavoritesEvent.Submit)
            awaitIds(listOf("k"))

            send(FavoritesEvent.InputChanged("   "))
            send(FavoritesEvent.Submit)
            val all = awaitIds(listOf("a", "k"))
            assertEquals("", all.submittedQuery)
        }
    }

    @Test
    fun `가격 필터는 양끝을 포함하고 가격 정보가 없는 도서는 제외한다`() {
        favoriteBooks = listOf(kotlin, android, git, compose)
        runPresenterTest {
            send(FavoritesEvent.PriceFilterApplied(PriceFilter(min = 30_000, max = 36_000)))
            awaitIds(listOf("a", "k"))

            send(FavoritesEvent.PriceFilterApplied(PriceFilter(min = 0, max = null)))
            awaitIds(listOf("a", "k"))

            send(FavoritesEvent.PriceFilterCleared)
            val cleared = awaitIds(listOf("c", "g", "a", "k"))
            assertEquals(PriceFilter.None, cleared.priceFilter)
        }
    }

    @Test
    fun `최소가 최대보다 큰 범위는 적용하지 않는다`() {
        favoriteBooks = listOf(kotlin, android)
        runPresenterTest {
            send(FavoritesEvent.PriceFilterApplied(PriceFilter(max = 30_000)))
            awaitIds(listOf("a"))

            send(FavoritesEvent.PriceFilterApplied(PriceFilter(min = 40_000, max = 30_000)))
            send(FavoritesEvent.SortSelected(FavoriteSort.TitleDescending))
            val state = awaitItemMatching { it.sort == FavoriteSort.TitleDescending }
            assertEquals(PriceFilter(max = 30_000), state.priceFilter)
            assertEquals(listOf("a"), (state.content as FavoritesContent.Results).books.map(Book::id))
        }
    }

    @Test
    fun `조건에 맞는 도서가 없으면 즐겨찾기 없음과 구분한다`() {
        favoriteBooks = listOf(kotlin)
        runPresenterTest {
            send(FavoritesEvent.InputChanged("없는 제목"))
            send(FavoritesEvent.Submit)
            awaitItemMatching { it.content == FavoritesContent.NoMatch }

            favoriteBooks = emptyList()
            awaitItemMatching { it.content == FavoritesContent.NoFavorites }
        }
    }

    @Test
    fun `Room 목록이 바뀌면 현재 조건으로 바로 다시 그린다`() {
        favoriteBooks = listOf(kotlin)
        runPresenterTest {
            send(FavoritesEvent.SortSelected(FavoriteSort.TitleDescending))
            awaitIds(listOf("k"))

            favoriteBooks = listOf(kotlin, android, compose)
            awaitIds(listOf("k", "a", "c"))
        }
    }

    @Test
    fun `하트는 해제 명령을 쓰고 Room에서 빠지면 목록에서 사라진다`() {
        val gate = CompletableDeferred<Unit>()
        writer.write = { gate.await() }
        favoriteBooks = listOf(kotlin, android)
        runPresenterTest {
            awaitIds(listOf("a", "k"))

            send(FavoritesEvent.ToggleFavorite(kotlin))
            val pending = awaitItemMatching { it.pendingFavoriteIds.isNotEmpty() }
            assertEquals(setOf("k"), pending.pendingFavoriteIds)

            gate.complete(Unit)
            favoriteBooks = listOf(android)
            awaitItemMatching {
                it.pendingFavoriteIds.isEmpty() &&
                    (it.content as? FavoritesContent.Results)?.books?.map(Book::id) == listOf("a")
            }
            assertEquals(listOf(FavoriteCommand(kotlin, favorite = false)), writer.commands)
        }
    }

    @Test
    fun `즐겨찾기 해제가 실패하면 목록을 유지하고 메시지를 한 번 보여 준다`() {
        writer.write = { throw IOException("disk full") }
        favoriteBooks = listOf(kotlin)
        runPresenterTest {
            awaitIds(listOf("k"))
            send(FavoritesEvent.ToggleFavorite(kotlin))

            val failed = awaitItemMatching { it.userMessage != null }
            assertEquals(ErrorMessage.FavoriteWrite, failed.userMessage)
            assertEquals(listOf("k"), (failed.content as FavoritesContent.Results).books.map(Book::id))

            send(FavoritesEvent.UserMessageShown)
            awaitItemMatching { it.userMessage == null }
        }
    }
}
