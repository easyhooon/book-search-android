package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.model.SearchPage
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.network.KakaoBookService
import kotlinx.coroutines.CancellationException
import soil.query.InfiniteQueryId
import soil.query.InfiniteQueryKey
import soil.query.QueryOptionsOverride
import soil.query.buildInfiniteQueryKey
import soil.query.copy
import kotlin.time.Duration.Companion.minutes

/** 검색어와 정렬이 같으면 같은 캐시를 공유합니다. 검색어는 앞뒤 공백을 제거한 값입니다. */
class SearchBooksQueryId(
    query: String,
    sort: SearchSort,
) : InfiniteQueryId<SearchPage, Int>(
    namespace = "search-books",
    "query" to query,
    "sort" to sort.name,
)

fun interface SearchBooksQueryKeyFactory {
    fun create(query: String, sort: SearchSort): InfiniteQueryKey<SearchPage, Int>
}

class DefaultSearchBooksQueryKeyFactory(
    private val service: KakaoBookService,
) : SearchBooksQueryKeyFactory {
    override fun create(query: String, sort: SearchSort): InfiniteQueryKey<SearchPage, Int> =
        SearchBooksQueryKey(service, query, sort)
}

/** Soil의 재조회/추가 로딩 공통 오류에서 실패한 요청 페이지를 구별합니다. */
class SearchPageFetchException(
    val page: Int,
    override val cause: Exception,
) : Exception("Failed to fetch search page $page", cause)

internal class SearchBooksQueryKey(
    service: KakaoBookService,
    query: String,
    sort: SearchSort,
) : InfiniteQueryKey<SearchPage, Int> by buildInfiniteQueryKey(
    id = SearchBooksQueryId(query, sort),
    fetch = { page ->
        try {
            service.searchBooks(
                query = query,
                sort = sort.apiValue,
                page = page,
                size = SearchPaging.PAGE_SIZE,
            ).toSearchPage(page)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw SearchPageFetchException(page, error)
        }
    },
    initialParam = { SearchPaging.FIRST_PAGE },
    loadMoreParam = { chunks -> chunks.lastOrNull()?.data?.let(SearchPaging::nextPage) },
) {
    override fun onConfigureOptions(): QueryOptionsOverride = { options ->
        options.copy(
            staleTime = 5.minutes,
            shouldRetry = { error -> error.toErrorKind().isRetryable },
        )
    }
}

internal val SearchSort.apiValue: String
    get() = when (this) {
        SearchSort.Accuracy -> "accuracy"
        SearchSort.Latest -> "latest"
    }
