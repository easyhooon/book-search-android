package io.github.easyhooon.booksearch.feature.favorites

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceInputTest {

    @Test
    fun `숫자 외 문자는 버린다`() {
        assertEquals("12000", PriceInput.sanitize("12,000원"))
        assertEquals("", PriceInput.sanitize("-"))
        assertEquals("", PriceInput.sanitize("１２"))
    }

    @Test
    fun `9자리까지만 받아 Int 범위를 넘지 않는다`() {
        val sanitized = PriceInput.sanitize("9999999999")
        assertEquals("999999999", sanitized)
        assertEquals(999_999_999, PriceInput.parse(sanitized))
    }

    @Test
    fun `빈칸은 제한 없음이고 0은 유효한 금액이다`() {
        assertNull(PriceInput.parse(""))
        assertEquals(0, PriceInput.parse("0"))
        assertEquals(0, PriceInput.parse("000"))
    }
}
