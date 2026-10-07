package io.github.easyhooon.booksearch.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BookFormattersTest {

    @Test
    fun `가격은 천 단위 구분 기호와 원을 붙인다`() {
        assertEquals("0원", BookFormatters.price(0))
        assertEquals("999원", BookFormatters.price(999))
        assertEquals("16,200원", BookFormatters.price(16_200))
        assertEquals("999,999,999원", BookFormatters.price(999_999_999))
    }

    @Test
    fun `날짜는 점으로 구분한다`() {
        assertEquals("2024.01.05", BookFormatters.date(LocalDate.of(2024, 1, 5)))
    }

    @Test
    fun `이름 목록은 쉼표로 연결하고 비면 null이다`() {
        assertEquals("가, 나", BookFormatters.names(listOf("가", "나")))
        assertNull(BookFormatters.names(emptyList()))
    }
}
