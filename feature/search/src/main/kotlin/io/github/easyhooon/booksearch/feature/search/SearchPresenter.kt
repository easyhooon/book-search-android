package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.data.SearchPageFetchException
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import io.github.easyhooon.booksearch.core.ui.retainFavoriteToggler
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import soil.query.MutationKey
import soil.query.QueryFetchStatus
import soil.query.compose.InfiniteQueryLoadingErrorObject
import soil.query.compose.InfiniteQueryLoadingObject
import soil.query.compose.InfiniteQueryObject
import soil.query.compose.InfiniteQueryRefreshErrorObject
import soil.query.compose.InfiniteQuerySuccessObject
import soil.query.compose.LocalQueryClient
import soil.query.compose.rememberInfiniteQuery

@Immutable
data class SearchUiState(
    val input: String,
    val submittedQuery: String,
    val sort: SearchSort,
    val content: SearchContent,
    val favoriteIds: Set<String>,
    val pendingFavoriteIds: Set<String>,
    val userMessage: ErrorMessage?,
    val eventSink: (SearchEvent) -> Unit,
)

sealed interface SearchContent {
    data object Idle : SearchContent
    data object Loading : SearchContent
    data class Error(val message: ErrorMessage) : SearchContent
    data object Empty : SearchContent

    /** [loadMoreError]가 있으면 기존 목록을 유지한 채 하단에 재시도를 보여 줍니다. */
    data class Results(
        val books: List<Book>,
        val isLoadingMore: Boolean,
        val loadMoreError: ErrorMessage?,
    ) : SearchContent
}

sealed interface SearchEvent {
    data class InputChanged(val value: String) : SearchEvent
    data object Submit : SearchEvent
    data class SortSelected(val sort: SearchSort) : SearchEvent
    data object LoadMore : SearchEvent
    data object Retry : SearchEvent
    data class ToggleFavorite(val book: Book) : SearchEvent
    data object UserMessageShown : SearchEvent
}

/** 프로세스 재생성 뒤에도 복원하는 검색 화면의 클라이언트 상태입니다. */
@Serializable
private data class SearchSavedState(
    val input: String = "",
    val submittedQuery: String = "",
    val sort: SearchSort = SearchSort.Accuracy,
)

/**
 * 검색 화면의 상태를 출처별로 모아 화면 상태로 바꿉니다.
 *
 * 입력·확정 조건·정렬은 저장 데이터, 즐겨찾기 쓰기 진행과 붙잡아 둔 쿼리는 retain 데이터로 여기서 직접 둡니다.
 * 검색 결과는 확정 조건의 Soil InfiniteQuery, 즐겨찾기 ID는 Root가 Room Subscription에서 읽어 넘깁니다.
 * 검색 결과 key는 확정 검색어에 따라 바뀌므로 Root가 아니라 여기서 연결합니다.
 */
@Composable
fun searchPresenter(
    queryKeyFactory: SearchBooksQueryKeyFactory,
    favoriteIds: Set<String>,
    favoriteMutationKey: MutationKey<Unit, FavoriteCommand>,
): SearchUiState {
    var saved by rememberSerializable { mutableStateOf(SearchSavedState()) }
    val favoriteToggler = retainFavoriteToggler(favoriteMutationKey)
    val queryHolder = retainSearchQueryHolder()
    val scope = rememberCoroutineScope()
    val queryClient = LocalQueryClient.current

    val query: InfiniteQueryObject<List<Book>, Int>? = if (saved.submittedQuery.isEmpty()) {
        null
    } else {
        val queryKey = remember(saved.submittedQuery, saved.sort) {
            queryKeyFactory.create(saved.submittedQuery, saved.sort)
        }
        SideEffect { queryHolder.hold(queryClient, queryKey) }
        // Soil의 기본 수집은 key 없는 collectAsState라서 조건이 바뀐 첫 프레임에 이전 결과를 돌려줍니다.
        // 조건별로 그룹을 나눠 새 조건은 항상 자기 상태에서 시작하게 합니다.
        key(queryKey.id) {
            rememberInfiniteQuery(queryKey, select = { chunks -> SearchPaging.merge(chunks.map { it.data }) })
        }
    }

    if (query == null) SideEffect { queryHolder.release() }

    fun retry() {
        when (query) {
            is InfiniteQueryLoadingErrorObject -> scope.launch { query.refresh() }
            is InfiniteQueryRefreshErrorObject -> {
                val param = query.loadMoreParam
                val failedPage = (query.error as? SearchPageFetchException)?.page
                // 다음 페이지가 있어도 기존 페이지의 재조회가 실패했다면 refresh해야 합니다.
                scope.launch { if (param != null && failedPage == param) query.loadMore(param) else query.refresh() }
            }
            else -> Unit
        }
    }

    return SearchUiState(
        input = saved.input,
        submittedQuery = saved.submittedQuery,
        sort = saved.sort,
        content = query.toContent(),
        favoriteIds = favoriteIds,
        pendingFavoriteIds = favoriteToggler.pendingIds.toSet(),
        userMessage = if (favoriteToggler.hasFailure) ErrorMessage.FavoriteWrite else null,
    ) { event ->
        when (event) {
            is SearchEvent.InputChanged -> saved = saved.copy(input = event.value)
            SearchEvent.Submit -> {
                val submitted = saved.input.trim()
                // 같은 조건을 다시 확정하면 새로 요청하지 않고 오류일 때만 재시도합니다.
                if (submitted == saved.submittedQuery) retry() else saved = saved.copy(submittedQuery = submitted)
            }
            is SearchEvent.SortSelected -> saved = saved.copy(sort = event.sort)
            SearchEvent.LoadMore -> {
                // 같은 param의 중복 요청은 Soil이 actor에서 건너뜁니다.
                val param = query?.loadMoreParam
                if (query is InfiniteQuerySuccessObject && query.fetchStatus == QueryFetchStatus.Idle && param != null) {
                    scope.launch { query.loadMore(param) }
                }
            }
            SearchEvent.Retry -> retry()
            is SearchEvent.ToggleFavorite ->
                favoriteToggler.setFavorite(book = event.book, favorite = event.book.id !in favoriteIds)
            SearchEvent.UserMessageShown -> favoriteToggler.consumeFailure()
        }
    }
}

/** 재시도 중(Fetching)에는 직전 오류를 지우고 로딩으로 보여 줍니다. */
private fun InfiniteQueryObject<List<Book>, Int>?.toContent(): SearchContent {
    if (this == null) return SearchContent.Idle
    val isFetching = fetchStatus is QueryFetchStatus.Fetching
    return when (this) {
        is InfiniteQueryLoadingObject -> SearchContent.Loading
        is InfiniteQueryLoadingErrorObject ->
            if (isFetching) SearchContent.Loading else SearchContent.Error(error.toSearchErrorMessage())
        is InfiniteQuerySuccessObject ->
            if (data.isEmpty()) {
                SearchContent.Empty
            } else {
                SearchContent.Results(books = data, isLoadingMore = isFetching, loadMoreError = null)
            }
        is InfiniteQueryRefreshErrorObject -> when {
            data.isNotEmpty() -> SearchContent.Results(
                books = data,
                isLoadingMore = isFetching,
                loadMoreError = if (isFetching) null else error.toSearchErrorMessage(),
            )
            isFetching -> SearchContent.Loading
            else -> SearchContent.Error(error.toSearchErrorMessage())
        }
    }
}
