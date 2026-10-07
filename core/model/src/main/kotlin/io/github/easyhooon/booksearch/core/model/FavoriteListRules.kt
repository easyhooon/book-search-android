package io.github.easyhooon.booksearch.core.model

import java.text.Collator
import java.util.Locale

/**
 * 즐겨찾기 탭의 로컬 검색·가격 필터·정렬 규칙입니다.
 *
 * 개인 즐겨찾기는 많아야 수백 건이라고 보고, 한국어 정렬과 테스트 용이성을 위해
 * Room에서 읽은 전체 목록을 메모리에서 처리합니다.
 */
object FavoriteListRules {

    fun apply(
        books: List<Book>,
        query: String,
        sort: FavoriteSort,
        priceFilter: PriceFilter,
    ): List<Book> {
        val normalizedQuery = query.trim()
        val filter = if (priceFilter.isValid) priceFilter else PriceFilter.None
        return books
            .filter { matchesQuery(it, normalizedQuery) && filter.matches(it.price) }
            .sortedWith(comparator(sort))
    }

    /** 제목 또는 저자 중 하나에 부분 일치하면 통과합니다. 영문 대소문자는 구분하지 않습니다. */
    fun matchesQuery(book: Book, query: String): Boolean {
        val normalized = query.trim()
        if (normalized.isEmpty()) return true
        return book.title.contains(normalized, ignoreCase = true) ||
            book.authors.any { it.contains(normalized, ignoreCase = true) }
    }

    fun comparator(sort: FavoriteSort): Comparator<Book> {
        val collator = Collator.getInstance(Locale.KOREAN)
        val ascending = Comparator<Book> { a, b -> collator.compare(a.title.trim(), b.title.trim()) }
            .thenBy { it.id }
        return when (sort) {
            FavoriteSort.TitleAscending -> ascending
            FavoriteSort.TitleDescending -> Comparator<Book> { a, b -> collator.compare(b.title.trim(), a.title.trim()) }
                .thenBy { it.id }
        }
    }
}
