package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import kotlinx.coroutines.launch
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

/**
 * 검색 화면의 상태를 출처별로 모아 화면 상태로 바꿉니다.
 *
 * 입력·확정 조건·정렬과 즐겨찾기 쓰기 진행은 [SearchStore], 검색 결과는 확정 조건의 Soil InfiniteQuery,
 * 즐겨찾기 ID는 Root가 Room Subscription에서 읽어 넘깁니다. 검색 결과 key는 확정 검색어에 따라 바뀌므로
 * Root가 아니라 여기서 연결합니다.
 */
@Composable
fun searchPresenter(
    queryKeyFactory: SearchBooksQueryKeyFactory,
    favoriteIds: Set<String>,
    favoriteMutationKey: MutationKey<Unit, FavoriteCommand>,
): SearchUiState {
    val store = rememberSearchStore(favoriteMutationKey)
    val state = store.state
    val scope = rememberCoroutineScope()
    val queryClient = LocalQueryClient.current

    val query: InfiniteQueryObject<List<Book>, Int>? = if (state.submittedQuery.isEmpty()) {
        null
    } else {
        val queryKey = remember(state.submittedQuery, state.sort) {
            queryKeyFactory.create(state.submittedQuery, state.sort)
        }
        SideEffect { store.holdQuery(queryClient, queryKey) }
        // Soil의 기본 수집은 key 없는 collectAsState라서 조건이 바뀐 첫 프레임에 이전 결과를 돌려줍니다.
        // 조건별로 그룹을 나눠 새 조건은 항상 자기 상태에서 시작하게 합니다.
        key(queryKey.id) {
            rememberInfiniteQuery(queryKey, select = { chunks -> SearchPaging.merge(chunks.map { it.data }) })
        }
    }

    if (query == null) SideEffect { store.releaseQuery() }

    val server = object : SearchServer {
        override val favoriteIds: Set<String> = favoriteIds

        override fun retry() {
            when (query) {
                is InfiniteQueryLoadingErrorObject -> scope.launch { query.refresh() }
                is InfiniteQueryRefreshErrorObject -> {
                    val param = query.loadMoreParam
                    scope.launch { if (param != null) query.loadMore(param) else query.refresh() }
                }
                else -> Unit
            }
        }

        override fun loadMore() {
            // 같은 param의 중복 요청은 Soil이 actor에서 건너뜁니다.
            val param = query?.loadMoreParam
            if (query is InfiniteQuerySuccessObject && query.fetchStatus == QueryFetchStatus.Idle && param != null) {
                scope.launch { query.loadMore(param) }
            }
        }
    }

    return SearchUiState(
        input = state.input,
        submittedQuery = state.submittedQuery,
        sort = state.sort,
        content = query.toContent(),
        favoriteIds = favoriteIds,
        pendingFavoriteIds = store.pendingFavoriteIds.toSet(),
        userMessage = if (store.hasFavoriteFailure) ErrorMessage.FavoriteWrite else null,
        eventSink = { event -> store.dispatch(event, server) },
    )
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
