package io.github.easyhooon.booksearch.core.model

/**
 * 원격 검색 한 페이지의 결과입니다. [isEnd]는 API의 `meta.is_end`이며,
 * 다음 페이지 판단은 중복 제거 전 응답 기준으로 합니다.
 */
data class SearchPage(
    val page: Int,
    val books: List<Book>,
    val isEnd: Boolean,
    val totalCount: Int,
)

object SearchPaging {
    const val PAGE_SIZE = 20
    const val FIRST_PAGE = 1
    const val MAX_PAGE = 50

    /** 남은 아이템이 이 개수 이하로 보이면 다음 페이지를 요청합니다. */
    const val PREFETCH_DISTANCE = 5

    fun nextPage(last: SearchPage): Int? =
        if (last.isEnd || last.page >= MAX_PAGE) null else last.page + 1

    /** 여러 페이지를 합치면서 같은 ID는 처음 나온 항목만 남깁니다. */
    fun merge(pages: List<SearchPage>): List<Book> {
        val seen = HashSet<String>()
        val merged = ArrayList<Book>(pages.sumOf { it.books.size })
        for (page in pages) {
            for (book in page.books) {
                if (seen.add(book.id)) merged += book
            }
        }
        return merged
    }

    fun shouldLoadMore(lastVisibleIndex: Int, itemCount: Int): Boolean =
        itemCount > 0 && lastVisibleIndex >= itemCount - 1 - PREFETCH_DISTANCE
}
