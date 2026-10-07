package io.github.easyhooon.booksearch.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteListRulesTest {

    private val books = listOf(
        testBook("c", title = "코틀린 인 액션", authors = listOf("Dmitry Jemerov"), price = 36_000),
        testBook("a", title = "Android 프로그래밍", authors = listOf("홍길동"), price = 30_000),
        testBook("b", title = "가나다", authors = listOf("김철수"), price = null),
        testBook("d", title = "가나다", authors = listOf("이영희"), price = 15_000),
    )

    @Test
    fun `제목 또는 저자 부분 일치이며 대소문자를 무시한다`() {
        val byTitle = FavoriteListRules.apply(books, "android", FavoriteSort.TitleAscending, PriceFilter.None)
        assertEquals(listOf("a"), byTitle.map { it.id })
        val byAuthor = FavoriteListRules.apply(books, " jemerov ", FavoriteSort.TitleAscending, PriceFilter.None)
        assertEquals(listOf("c"), byAuthor.map { it.id })
    }

    @Test
    fun `제목 오름차순 동률이면 ID 순서로 고정한다`() {
        val result = FavoriteListRules.apply(books, "", FavoriteSort.TitleAscending, PriceFilter.None)
        assertEquals(listOf("a", "b", "d", "c"), result.map { it.id })
    }

    @Test
    fun `제목 내림차순에서도 동률은 ID 오름차순이다`() {
        val result = FavoriteListRules.apply(books, "", FavoriteSort.TitleDescending, PriceFilter.None)
        assertEquals(listOf("c", "b", "d", "a"), result.map { it.id })
    }

    @Test
    fun `가격 필터가 활성화되면 가격 정보가 없는 도서를 제외한다`() {
        val result = FavoriteListRules.apply(books, "", FavoriteSort.TitleAscending, PriceFilter(max = 30_000))
        assertEquals(setOf("a", "d"), result.map { it.id }.toSet())
    }

    @Test
    fun `유효하지 않은 가격 범위는 적용하지 않는다`() {
        val result = FavoriteListRules.apply(books, "", FavoriteSort.TitleAscending, PriceFilter(min = 5, max = 1))
        assertEquals(books.size, result.size)
    }
}
