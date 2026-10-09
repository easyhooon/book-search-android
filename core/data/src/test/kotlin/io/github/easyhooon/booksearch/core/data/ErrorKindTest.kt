package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.network.MissingApiKeyException
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ErrorKindTest {

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `페이지 정보로 감싼 오류도 원인의 분류와 재시도 정책을 유지한다`() {
        listOf(
            IOException(), MissingApiKeyException(), http(401), http(403), http(400),
            http(429), http(500), SerializationException("x"), IllegalStateException(),
        ).forEach { cause ->
            val wrapped = SearchPageFetchException(page = 2, cause = cause)
            assertEquals(cause.toErrorKind(), wrapped.toErrorKind())
            assertEquals(cause.toErrorKind().isRetryable, wrapped.toErrorKind().isRetryable)
        }
    }

    @Test
    fun `키 누락은 IOException 하위여도 재시도하지 않는다`() {
        val kind = MissingApiKeyException().toErrorKind()
        assertEquals(ErrorKind.MissingApiKey, kind)
        assertFalse(kind.isRetryable)
    }

    @Test
    fun `일시적 오류만 재시도한다`() {
        assertTrue(IOException().toErrorKind().isRetryable)
        assertTrue(http(500).toErrorKind().isRetryable)
        assertTrue(http(503).toErrorKind().isRetryable)
        assertTrue(http(429).toErrorKind().isRetryable)
    }

    @Test
    fun `인증 오류와 클라이언트 오류와 파싱 오류는 재시도하지 않는다`() {
        assertEquals(ErrorKind.Unauthorized, http(401).toErrorKind())
        assertEquals(ErrorKind.Unauthorized, http(403).toErrorKind())
        assertEquals(ErrorKind.Client, http(400).toErrorKind())
        assertEquals(ErrorKind.Parse, SerializationException("x").toErrorKind())
        assertEquals(ErrorKind.Unknown, IllegalStateException().toErrorKind())
        listOf(http(401), http(400), SerializationException("x"), IllegalStateException())
            .forEach { assertFalse(it.toErrorKind().isRetryable) }
    }
}
