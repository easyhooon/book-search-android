package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import io.github.easyhooon.booksearch.core.ui.retainFavoriteToggler
import kotlinx.coroutines.launch
import soil.query.InfiniteQueryId
import soil.query.InfiniteQueryKey
import soil.query.MutationKey
import soil.query.QueryClient
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
 * 검색 화면의 입력·확정 조건·정렬을 들고, 확정 조건의 InfiniteQuery를 화면 상태로 바꿉니다.
 *
 * 검색 결과 key는 확정 검색어에 따라 바뀌므로 Root가 아니라 여기서 연결합니다.
 * 즐겨찾기 ID는 Root가 Room Subscription에서 읽어 넘깁니다.
 */
@Composable
fun searchPresenter(
    queryKeyFactory: SearchBooksQueryKeyFactory,
    favoriteIds: Set<String>,
    favoriteMutationKey: MutationKey<Unit, FavoriteCommand>,
): SearchUiState {
    var input by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SearchSort.Accuracy) }
    val favoriteToggler = retainFavoriteToggler(favoriteMutationKey)
    val scope = rememberCoroutineScope()
    val queryHolder = retain { SearchQueryHolder() }
    val queryClient = LocalQueryClient.current

    val query: InfiniteQueryObject<List<Book>, Int>? = if (submittedQuery.isEmpty()) {
        null
    } else {
        val queryKey = remember(submittedQuery, sort) { queryKeyFactory.create(submittedQuery, sort) }
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
                scope.launch { if (param != null) query.loadMore(param) else query.refresh() }
            }
            else -> Unit
        }
    }

    return SearchUiState(
        input = input,
        submittedQuery = submittedQuery,
        sort = sort,
        content = query.toContent(),
        favoriteIds = favoriteIds,
        pendingFavoriteIds = favoriteToggler.pendingIds.toSet(),
        userMessage = if (favoriteToggler.hasFailure) ErrorMessage.FavoriteWrite else null,
    ) { event ->
        when (event) {
            is SearchEvent.InputChanged -> input = event.value
            SearchEvent.Submit -> {
                val trimmed = input.trim()
                // 같은 조건을 다시 확정하면 새로 요청하지 않고 오류일 때만 재시도합니다.
                if (trimmed == submittedQuery) retry() else submittedQuery = trimmed
            }
            is SearchEvent.SortSelected -> sort = event.sort
            SearchEvent.LoadMore -> {
                // 같은 param의 중복 요청은 Soil이 actor에서 건너뜁니다.
                val param = query?.loadMoreParam
                if (query is InfiniteQuerySuccessObject && query.fetchStatus == QueryFetchStatus.Idle && param != null) {
                    scope.launch { query.loadMore(param) }
                }
            }
            SearchEvent.Retry -> retry()
            is SearchEvent.ToggleFavorite -> favoriteToggler.setFavorite(
                book = event.book,
                favorite = event.book.id !in favoriteIds,
            )
            SearchEvent.UserMessageShown -> favoriteToggler.consumeFailure()
        }
    }
}

/**
 * 검색 루트가 백스택에 있는 동안 확정 조건의 쿼리를 Soil에 붙여 둡니다.
 *
 * Soil은 구독이 모두 끊기고 keepAliveTime(기본 5초)이 지나면 쿼리를 내리면서 성공 상태만 캐시에 남깁니다.
 * 즐겨찾기 탭으로 가면 검색 루트는 컴포지션에서 빠지므로, 추가 로딩이 실패한 목록은 돌아왔을 때 지워져
 * 1페이지부터 다시 받게 됩니다. 엔트리 범위로 retain한 참조를 따로 붙잡아 이를 막고, 조건이 바뀌거나
 * 검색어를 비우거나 엔트리가 은퇴하면 놓습니다. 놓은 쿼리는 Soil의 기본 정책대로 정리됩니다.
 */
private class SearchQueryHolder : RetainObserver {
    private var ref: AutoCloseable? = null
    private var id: InfiniteQueryId<*, *>? = null

    fun <T, S> hold(client: QueryClient, key: InfiniteQueryKey<T, S>) {
        if (key.id == id) return
        release()
        ref = client.getInfiniteQuery(key)
        id = key.id
    }

    fun release() {
        ref?.close()
        ref = null
        id = null
    }

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() = release()

    override fun onUnused() = release()
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
