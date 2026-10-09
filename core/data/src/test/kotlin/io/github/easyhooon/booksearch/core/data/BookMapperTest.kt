package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.network.model.DocumentResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BookMapperTest {

    @Test
    fun `정가 원본을 보존하고 양수만 표시 가능한 가격으로 구분한다`() {
        for (price in listOf(null, Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE)) {
            val book = DocumentResponse(price = price, isbn = "9788996991342").toBook()
            assertEquals(price, book.price)
            assertEquals(price?.takeIf { it > 0 }, book.availablePrice)
        }
    }

    @Test
    fun `판매가 매핑은 기존 정책을 유지한다`() {
        assertEquals(0, DocumentResponse(price = 10_000, salePrice = 0).toBook().salePrice)
        assertNull(DocumentResponse(price = 10_000, salePrice = -1).toBook().salePrice)
        assertNull(DocumentResponse(price = 10_000).toBook().salePrice)
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
