package io.github.easyhooon.booksearch.core.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class KakaoBookServiceTest {

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun service(apiKey: String = "test-key") = createKakaoBookService(
        baseClient = createBaseOkHttpClient(),
        config = NetworkConfig(apiKey = apiKey, enableHttpLog = false, baseUrl = server.url("/").toString()),
    )

    @Test
    fun `인증 헤더와 검색 파라미터를 보내고 알 수 없는 필드를 무시한다`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body(
                    """
                    {"meta":{"total_count":1,"pageable_count":1,"is_end":true,"unknown":1},
                     "documents":[{"title":"코틀린","isbn":"8996991341 9788996991342","price":-1,
                     "sale_price":null,"authors":["저자"],"extra":"x"}]}
                    """.trimIndent(),
                )
                .build(),
        )

        val response = service().searchBooks(query = "코틀린 입문", sort = "accuracy", page = 2, size = 20)

        val request = server.takeRequest()
        assertEquals("KakaoAK test-key", request.headers["Authorization"])
        val url = request.url
        assertEquals("/v3/search/book", url.encodedPath)
        assertEquals("코틀린 입문", url.queryParameter("query"))
        assertEquals("accuracy", url.queryParameter("sort"))
        assertEquals("2", url.queryParameter("page"))
        assertEquals("20", url.queryParameter("size"))

        assertTrue(response.meta.isEnd)
        val document = response.documents.single()
        assertEquals(-1, document.price)
        assertNull(document.salePrice)
        assertEquals("", document.thumbnail)
    }

    @Test
    fun `키가 비어 있으면 요청을 보내지 않고 MissingApiKeyException을 던진다`() = runTest {
        try {
            service(apiKey = " ").searchBooks("a", "accuracy", 1, 20)
            fail("예외가 발생해야 합니다")
        } catch (expected: MissingApiKeyException) {
            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun `HTTP 오류는 HttpException으로 전파한다`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).build())
        try {
            service().searchBooks("a", "accuracy", 1, 20)
            fail("예외가 발생해야 합니다")
        } catch (expected: HttpException) {
            assertEquals(429, expected.code())
        }
    }

    @Test
    fun `설정 문자열에 키를 노출하지 않는다`() {
        val text = NetworkConfig(apiKey = "secret-value", enableHttpLog = true).toString()
        assertFalse(text.contains("secret-value"))
    }
}
