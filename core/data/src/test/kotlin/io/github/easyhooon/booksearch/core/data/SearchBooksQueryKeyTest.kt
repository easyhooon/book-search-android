package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.network.KakaoBookService
import io.github.easyhooon.booksearch.core.network.NetworkConfig
import io.github.easyhooon.booksearch.core.network.model.SearchBookResponse
import io.github.easyhooon.booksearch.core.network.createBaseOkHttpClient
import io.github.easyhooon.booksearch.core.network.createKakaoBookService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import soil.query.QueryChunk
import soil.query.QueryOptions
import soil.query.QueryReceiver
import java.io.IOException
import kotlin.time.Duration.Companion.minutes

class SearchBooksQueryKeyTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun key(query: String = "코틀린", sort: SearchSort = SearchSort.Latest) =
        DefaultSearchBooksQueryKeyFactory(
            createKakaoBookService(
                createBaseOkHttpClient(),
                NetworkConfig(apiKey = "test-key", enableHttpLog = false, baseUrl = server.url("/").toString()),
            ),
        ).create(query, sort)

    @Test
    fun `페이지 파라미터로 요청하고 응답을 페이지 모델로 바꾼다`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body("""{"meta":{"total_count":41,"is_end":false},"documents":[{"title":"A","isbn":"9788996991342"}]}""")
                .build(),
        )
        val key = key()

        val page = key.fetch(QueryReceiver, 3)

        val url = server.takeRequest().url
        assertEquals("latest", url.queryParameter("sort"))
        assertEquals("3", url.queryParameter("page"))
        assertEquals("20", url.queryParameter("size"))
        assertEquals(3, page.page)
        assertFalse(page.isEnd)
        assertEquals("isbn13:9788996991342", page.books.single().id)

        assertEquals(1, key.initialParam())
        assertEquals(4, key.loadMoreParam(listOf(QueryChunk(page, 3))))
        assertNull(key.loadMoreParam(listOf(QueryChunk(page.copy(isEnd = true), 3))))
    }

    @Test
    fun `실패한 페이지와 원인을 보존한다`() = runTest {
        server.enqueue(MockResponse.Builder().code(503).build())
        val error = runCatching { key().fetch(QueryReceiver, 3) }.exceptionOrNull()

        assertTrue(error is SearchPageFetchException)
        assertEquals(3, (error as SearchPageFetchException).page)
        assertEquals(ErrorKind.Server, error.toErrorKind())
        assertTrue(key().onConfigureOptions()!!.invoke(QueryOptions).shouldRetry(error))
    }

    @Test
    fun `요청 취소는 페이지 오류로 감싸지 않는다`() = runTest {
        val cancellation = CancellationException("cancelled")
        val service = object : KakaoBookService {
            override suspend fun searchBooks(query: String, sort: String, page: Int, size: Int): SearchBookResponse =
                throw cancellation
        }
        val key = DefaultSearchBooksQueryKeyFactory(service).create("코틀린", SearchSort.Accuracy)

        val error = runCatching { key.fetch(QueryReceiver, 2) }.exceptionOrNull()

        assertSame(cancellation, error)
    }

    @Test
    fun `같은 검색어와 정렬은 같은 ID이고 정렬이 다르면 다른 ID이다`() {
        assertEquals(key().id, key().id)
        assertFalse(key(sort = SearchSort.Latest).id == key(sort = SearchSort.Accuracy).id)
    }

    @Test
    fun `5분 stale과 일시적 오류만 재시도하는 옵션을 쓴다`() {
        val options = key().onConfigureOptions()!!.invoke(QueryOptions)
        assertEquals(5.minutes, options.staleTime)
        assertTrue(options.shouldRetry(IOException()))
        assertFalse(options.shouldRetry(io.github.easyhooon.booksearch.core.network.MissingApiKeyException()))
    }
}
