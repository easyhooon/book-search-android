package io.github.easyhooon.booksearch.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookIdentityTest {

    private fun of(isbn: String, url: String = "https://example.com/b") =
        BookIdentity.of(isbn, url, title = "제목", publisher = "출판사", rawDatetime = "2024-01-01T00:00:00.000+09:00")

    @Test
    fun `ISBN10과 ISBN13이 모두 있으면 ISBN13을 ID로 쓴다`() {
        val identity = of("8996991341 9788996991342")
        assertEquals("isbn13:9788996991342", identity.id)
        assertEquals("8996991341", identity.isbn10)
        assertEquals("9788996991342", identity.isbn13)
    }

    @Test
    fun `순서가 바뀌어도 길이로 구분한다`() {
        assertEquals("isbn13:9788996991342", of("9788996991342 8996991341").id)
    }

    @Test
    fun `ISBN10만 있으면 ISBN10을 쓰고 X 체크 문자를 대문자로 보존한다`() {
        val identity = of(" 899699134x")
        assertEquals("isbn10:899699134X", identity.id)
        assertNull(identity.isbn13)
    }

    @Test
    fun `하이픈은 제거한다`() {
        assertEquals("isbn13:9788996991342", of("978-89-969913-4-2").id)
    }

    @Test
    fun `ISBN이 없으면 URL을 쓴다`() {
        assertEquals("url:https://example.com/b", of("", url = " https://example.com/b ").id)
    }

    @Test
    fun `길이가 맞지 않는 토큰은 ISBN으로 보지 않는다`() {
        assertEquals("url:https://example.com/b", of("12345").id)
    }

    @Test
    fun `ISBN과 URL이 모두 없으면 제목 출판사 발간일 조합을 쓴다`() {
        assertEquals("fallback:제목|출판사|2024-01-01T00:00:00.000+09:00", of("", url = "").id)
    }
}
