package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.network.model.DocumentResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BookMapperTest {

    @Test
    fun `음수 가격은 가격 정보 없음이고 0은 유지한다`() {
        val book = DocumentResponse(price = -1, salePrice = 0, isbn = "9788996991342").toBook()
        assertNull(book.price)
        assertEquals(0, book.salePrice)
    }

    @Test
    fun `발간일은 오프셋 날짜시간에서 날짜만 사용한다`() {
        assertEquals(LocalDate.of(2014, 11, 17), parsePublishedDate("2014-11-17T00:00:00.000+09:00"))
        assertEquals(LocalDate.of(2014, 11, 17), parsePublishedDate("2014-11-17"))
        assertNull(parsePublishedDate(""))
        assertNull(parsePublishedDate("invalid"))
    }

    @Test
    fun `ISBN과 문자열 필드를 정리한다`() {
        val book = DocumentResponse(
            title = " 제목 ",
            isbn = "8996991341 9788996991342",
            authors = listOf("저자", " "),
            url = "https://example.com",
        ).toBook()
        assertEquals("isbn13:9788996991342", book.id)
        assertEquals("8996991341", book.isbn10)
        assertEquals("제목", book.title)
        assertEquals(listOf("저자"), book.authors)
    }
}
