package io.github.easyhooon.booksearch.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPagingTest {

    private fun page(page: Int, ids: List<String>, isEnd: Boolean = false) =
        SearchPage(page = page, books = ids.map { testBook(it) }, isEnd = isEnd, totalCount = 1000)

    @Test
    fun `is_end이면 다음 페이지가 없다`() {
        assertNull(SearchPaging.nextPage(page(3, listOf("a"), isEnd = true)))
    }

    @Test
    fun `50페이지에 도달하면 is_end가 false여도 멈춘다`() {
        assertEquals(50, SearchPaging.nextPage(page(49, listOf("a"))))
        assertNull(SearchPaging.nextPage(page(50, listOf("a"))))
    }

    @Test
    fun `중복 제거로 페이지가 비어도 다음 페이지 판단은 응답 기준이다`() {
        val first = page(1, listOf("a", "b"))
        val second = page(2, listOf("a", "b"))
        assertEquals(listOf("a", "b"), SearchPaging.merge(listOf(first, second)).map { it.id })
        assertEquals(3, SearchPaging.nextPage(second))
    }

    @Test
    fun `중복 ID는 처음 나온 항목을 유지한다`() {
        val first = SearchPage(1, listOf(testBook("a", title = "처음")), false, 2)
        val second = SearchPage(2, listOf(testBook("a", title = "나중"), testBook("b")), false, 2)
        val merged = SearchPaging.merge(listOf(first, second))
        assertEquals(listOf("a", "b"), merged.map { it.id })
        assertEquals("처음", merged.first().title)
    }

    @Test
    fun `남은 아이템이 5개 이하이면 다음 페이지를 요청한다`() {
        assertFalse(SearchPaging.shouldLoadMore(lastVisibleIndex = 13, itemCount = 20))
        assertTrue(SearchPaging.shouldLoadMore(lastVisibleIndex = 14, itemCount = 20))
        assertFalse(SearchPaging.shouldLoadMore(lastVisibleIndex = 0, itemCount = 0))
    }
}

class BookTest {
    @Test
    fun `판매가가 정가보다 낮을 때만 할인가로 본다`() {
        assertEquals(8_000, testBook("a", price = 10_000, salePrice = 8_000).discountedPrice)
        assertNull(testBook("a", price = 10_000, salePrice = 10_000).discountedPrice)
        assertNull(testBook("a", price = 10_000, salePrice = null).discountedPrice)
        assertNull(testBook("a", price = null, salePrice = 8_000).discountedPrice)
        assertEquals(0, testBook("a", price = 10_000, salePrice = 0).discountedPrice)
    }
}
