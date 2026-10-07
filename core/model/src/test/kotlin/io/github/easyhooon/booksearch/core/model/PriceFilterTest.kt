package io.github.easyhooon.booksearch.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceFilterTest {

    @Test
    fun `범위가 없으면 가격 정보가 없는 도서도 통과한다`() {
        assertTrue(PriceFilter.None.matches(null))
        assertTrue(PriceFilter.None.matches(0))
    }

    @Test
    fun `양 끝값을 포함한다`() {
        val filter = PriceFilter(min = 10_000, max = 20_000)
        assertTrue(filter.matches(10_000))
        assertTrue(filter.matches(20_000))
        assertFalse(filter.matches(9_999))
        assertFalse(filter.matches(20_001))
    }

    @Test
    fun `한쪽만 지정하면 다른 쪽은 제한하지 않는다`() {
        assertTrue(PriceFilter(min = 0).matches(0))
        assertTrue(PriceFilter(max = 0).matches(0))
        assertFalse(PriceFilter(max = 0).matches(1))
    }

    @Test
    fun `범위가 지정되면 가격 정보가 없는 도서를 제외한다`() {
        assertFalse(PriceFilter(min = 0).matches(null))
    }

    @Test
    fun `최솟값이 최댓값보다 크면 유효하지 않다`() {
        assertFalse(PriceFilter(min = 2, max = 1).isValid)
        assertTrue(PriceFilter(min = 1, max = 1).isValid)
    }
}
