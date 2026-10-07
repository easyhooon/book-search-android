package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryId
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.SearchPage
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.testing.MainDispatcherRule
import io.github.easyhooon.booksearch.core.testing.awaitItemMatching
import io.github.easyhooon.booksearch.core.testing.presenterFlow
import io.github.easyhooon.booksearch.core.testing.testBook
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import soil.query.InfiniteQueryKey
import soil.query.MutationId
import soil.query.QueryOptionsOverride
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.buildInfiniteQueryKey
import soil.query.buildMutationKey
import soil.query.copy
import java.io.IOException

/** Android용 Compose runtime이 Trace·Log를 호출하므로 Robolectric에서 실행합니다. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSoilQueryApi::class)
class SearchPresenterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private data class Request(val query: String, val sort: SearchSort, val page: Int)

    /** 요청을 기록하고, 테스트가 정한 응답이나 대기 지점을 돌려주는 가짜 검색 API입니다. */
    private class FakeSearchApi {
        val requests = mutableListOf<Request>()
        var respond: suspend (Request) -> SearchPage = { page(it.page, isEnd = true) }

        val factory = SearchBooksQueryKeyFactory { query, sort -> key(query, sort) }

        private fun key(query: String, sort: SearchSort): InfiniteQueryKey<SearchPage, Int> {
            val base = buildInfiniteQueryKey(
                id = SearchBooksQueryId(query, sort),
                fetch = { page ->
                    val request = Request(query, sort, page)
                    requests += request
                    respond(request)
                },
                initialParam = { SearchPaging.FIRST_PAGE },
                loadMoreParam = { chunks -> chunks.lastOrNull()?.data?.let(SearchPaging::nextPage) },
            )
            // 자동 재시도를 끄고 오류 화면 전이만 확인합니다.
            return object : InfiniteQueryKey<SearchPage, Int> by base {
                override fun onConfigureOptions(): QueryOptionsOverride = { it.copy(shouldRetry = { false }) }
            }
        }
    }

    private class FakeFavoriteWriter {
        val commands = mutableListOf<FavoriteCommand>()
        var write: suspend (FavoriteCommand) -> Unit = {}
        val key = buildMutationKey<Unit, FavoriteCommand>(
            id = MutationId("favorite", "screen" to "test"),
            mutate = { command ->
                commands += command
                write(command)
            },
        )
    }

    private val api = FakeSearchApi()
    private val writer = FakeFavoriteWriter()
    private var favoriteIds by mutableStateOf(emptySet<String>())
    private lateinit var testScope: TestScope
    private lateinit var latest: SearchUiState

    private fun runPresenterTest(validate: suspend ReceiveTurbine<SearchUiState>.() -> Unit) = runTest {
        testScope = this
        val client = SwrCachePlus(backgroundScope)
        presenterFlow(client) {
            searchPresenter(
                queryKeyFactory = api.factory,
                favoriteIds = favoriteIds,
                favoriteMutationKey = writer.key,
            )
        }.onEach { latest = it }.test {
            validate()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 가장 최근 상태의 eventSink로 보냅니다. 방출 대기열은 소비하지 않습니다. */
    private fun send(event: SearchEvent) = latest.eventSink(event)

    private suspend fun ReceiveTurbine<SearchUiState>.search(query: String) {
        send(SearchEvent.InputChanged(query))
        awaitItemMatching { it.input == query }
        send(SearchEvent.Submit)
    }

    private suspend fun ReceiveTurbine<SearchUiState>.awaitResults(): SearchUiState =
        awaitItemMatching { it.content is SearchContent.Results }

    @Test
    fun `처음에는 검색 전 상태다`() = runPresenterTest {
        val state = awaitItem()
        assertEquals(SearchContent.Idle, state.content)
        assertEquals(SearchSort.Accuracy, state.sort)
    }

    @Test
    fun `검색어를 확정하면 로딩 뒤 1페이지 결과를 보여 준다`() {
        val gate = CompletableDeferred<Unit>()
        api.respond = { gate.await(); page(it.page, isEnd = false) }
        runPresenterTest {
            search(" 코틀린 ")
            awaitItemMatching { it.content == SearchContent.Loading }
            gate.complete(Unit)

            val content = awaitResults().content as SearchContent.Results
            assertEquals(listOf(Request("코틀린", SearchSort.Accuracy, 1)), api.requests)
            assertEquals(SearchPaging.PAGE_SIZE, content.books.size)
        }
    }

    @Test
    fun `공백만 확정하면 요청하지 않고 결과를 비운다`() = runPresenterTest {
        search("코틀린")
        awaitResults()
        send(SearchEvent.InputChanged("   "))
        send(SearchEvent.Submit)
        awaitItemMatching { it.content == SearchContent.Idle }
        assertEquals(1, api.requests.size)
    }

    @Test
    fun `결과가 없으면 빈 상태다`() {
        api.respond = { SearchPage(page = it.page, books = emptyList(), isEnd = true, totalCount = 0) }
        runPresenterTest {
            search("없는책")
            awaitItemMatching { it.content == SearchContent.Empty }
        }
    }

    @Test
    fun `성공한 조건을 다시 확정하면 새로 요청하지 않는다`() = runPresenterTest {
        search("코틀린")
        awaitResults()
        send(SearchEvent.Submit)
        testScope.runCurrent()

        assertEquals(1, api.requests.size)
    }

    @Test
    fun `다음 페이지를 합치면서 중복 ID는 처음 항목만 남긴다`() {
        api.respond = { request ->
            when (request.page) {
                1 -> page(1, isEnd = false, ids = (1..20).map { "id-$it" })
                else -> page(2, isEnd = true, ids = listOf("id-20", "id-21"))
            }
        }
        runPresenterTest {
            search("코틀린")
            awaitResults()
            send(SearchEvent.LoadMore)

            val content = awaitItemMatching { (it.content as? SearchContent.Results)?.books?.size == 21 }
                .content as SearchContent.Results
            assertEquals((1..21).map { "id-$it" }, content.books.map { it.id })
            assertEquals(listOf(1, 2), api.requests.map { it.page })
        }
    }

    @Test
    fun `추가 로딩 중에는 하단 로딩을 보여 주고 같은 요청을 겹치지 않는다`() {
        val gate = CompletableDeferred<Unit>()
        api.respond = { request ->
            if (request.page == 2) gate.await()
            page(request.page, isEnd = request.page == 2)
        }
        runPresenterTest {
            search("코틀린")
            awaitResults()
            send(SearchEvent.LoadMore)

            val loading = awaitItemMatching { (it.content as? SearchContent.Results)?.isLoadingMore == true }
            send(SearchEvent.LoadMore)
            send(SearchEvent.LoadMore)
            gate.complete(Unit)

            awaitItemMatching { (it.content as? SearchContent.Results)?.isLoadingMore == false }
            assertEquals(listOf(1, 2), api.requests.map { it.page })
        }
    }

    @Test
    fun `마지막 페이지에서는 더 요청하지 않는다`() = runPresenterTest {
        search("코틀린")
        awaitResults()
        send(SearchEvent.LoadMore)
        testScope.runCurrent()

        assertEquals(listOf(1), api.requests.map { it.page })
    }

    @Test
    fun `최초 오류는 같은 조건 재확정으로 다시 시도한다`() {
        var fail = true
        api.respond = { if (fail) throw IOException("offline") else page(it.page, isEnd = true) }
        runPresenterTest {
            search("코틀린")
            val error = awaitItemMatching { it.content is SearchContent.Error }
            assertEquals(SearchContent.Error(ErrorMessage.Network), error.content)

            fail = false
            send(SearchEvent.Submit)
            awaitResults()
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun `추가 오류는 기존 목록을 유지하고 같은 페이지를 재시도한다`() {
        var failPage2 = true
        api.respond = { request ->
            if (request.page == 2 && failPage2) throw IOException("offline")
            page(request.page, isEnd = request.page == 2)
        }
        runPresenterTest {
            search("코틀린")
            awaitResults()
            send(SearchEvent.LoadMore)

            val failed = awaitItemMatching { (it.content as? SearchContent.Results)?.loadMoreError != null }
            val content = failed.content as SearchContent.Results
            assertEquals(ErrorMessage.Network, content.loadMoreError)
            assertEquals(SearchPaging.PAGE_SIZE, content.books.size)

            failPage2 = false
            send(SearchEvent.Retry)
            awaitItemMatching { (it.content as? SearchContent.Results)?.books?.size == SearchPaging.PAGE_SIZE * 2 }
            assertEquals(listOf(1, 2, 2), api.requests.map { it.page })
        }
    }

    @Test
    fun `알 수 없는 오류는 일반 오류 문구로 보여 준다`() {
        api.respond = { throw IllegalStateException("boom") }
        runPresenterTest {
            search("코틀린")
            val error = awaitItemMatching { it.content is SearchContent.Error }
            assertEquals(SearchContent.Error(ErrorMessage.Generic), error.content)
        }
    }

    @Test
    fun `정렬을 바꾸면 같은 검색어로 1페이지부터 다시 검색한다`() = runPresenterTest {
        search("코틀린")
        awaitResults()
        send(SearchEvent.SortSelected(SearchSort.Latest))

        // 조건이 바뀐 첫 상태에 이전 정렬의 결과가 섞이지 않아야 합니다.
        assertEquals(SearchContent.Loading, awaitItemMatching { it.sort == SearchSort.Latest }.content)
        awaitItemMatching { it.content is SearchContent.Results }
        assertEquals(
            listOf(Request("코틀린", SearchSort.Accuracy, 1), Request("코틀린", SearchSort.Latest, 1)),
            api.requests,
        )
    }

    @Test
    fun `하트는 Room 상태의 반대를 목표 상태로 쓰고 진행 중에는 해당 도서만 잠근다`() {
        val gate = CompletableDeferred<Unit>()
        writer.write = { gate.await() }
        runPresenterTest {
            search("코틀린")
            val results = awaitResults()
            val book = (results.content as SearchContent.Results).books.first()

            send(SearchEvent.ToggleFavorite(book))
            val pending = awaitItemMatching { book.id in it.pendingFavoriteIds }
            assertEquals(setOf(book.id), pending.pendingFavoriteIds)

            gate.complete(Unit)
            favoriteIds = setOf(book.id)
            val saved = awaitItemMatching { it.pendingFavoriteIds.isEmpty() && book.id in it.favoriteIds }
            assertNull(saved.userMessage)

            send(SearchEvent.ToggleFavorite(book))
            awaitItemMatching { it.pendingFavoriteIds.isEmpty() && writer.commands.size == 2 }
            assertEquals(listOf(FavoriteCommand(book, true), FavoriteCommand(book, false)), writer.commands)
        }
    }

    @Test
    fun `즐겨찾기 쓰기가 실패하면 메시지를 한 번 보여 준다`() {
        writer.write = { throw IOException("disk full") }
        runPresenterTest {
            search("코틀린")
            val results = awaitResults()
            val book = (results.content as SearchContent.Results).books.first()

            send(SearchEvent.ToggleFavorite(book))
            val failed = awaitItemMatching { it.userMessage != null }
            assertEquals(ErrorMessage.FavoriteWrite, failed.userMessage)
            assertTrue(failed.pendingFavoriteIds.isEmpty())

            send(SearchEvent.UserMessageShown)
            awaitItemMatching { it.userMessage == null }
        }
    }

    private companion object {
        fun page(page: Int, isEnd: Boolean, ids: List<String>? = null): SearchPage {
            val bookIds = ids ?: (1..SearchPaging.PAGE_SIZE).map { "p$page-$it" }
            return SearchPage(
                page = page,
                books = bookIds.map { testBook(it) },
                isEnd = isEnd,
                totalCount = 1_000,
            )
        }
    }
}
