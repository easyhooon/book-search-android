package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSerializable
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.ui.FavoriteToggler
import io.github.easyhooon.booksearch.core.ui.retainFavoriteToggler
import kotlinx.serialization.Serializable
import soil.query.InfiniteQueryId
import soil.query.InfiniteQueryKey
import soil.query.MutationKey
import soil.query.QueryClient
import timber.log.Timber

/** 프로세스 재생성 뒤에도 복원하는 검색 화면의 클라이언트 상태입니다. */
@Serializable
internal data class SearchSavedState(
    val input: String = "",
    val submittedQuery: String = "",
    val sort: SearchSort = SearchSort.Accuracy,
)

/**
 * 검색 화면의 서버 쪽 상태와 동작입니다. Presenter가 컴포지션마다 만들어 [SearchStore.dispatch]에 넘깁니다.
 * InfiniteQuery 객체는 컴포지션 범위라서 Store는 이 값을 보관하지 않습니다.
 */
internal interface SearchServer {
    val favoriteIds: Set<String>
    fun retry()
    fun loadMore()
}

/**
 * 검색 화면의 클라이언트 상태를 한곳에서 바꿉니다.
 *
 * 저장 데이터는 [rememberSerializable], 진행 중인 즐겨찾기 쓰기와 붙잡아 둔 쿼리는 retain으로 두고,
 * Store 자체는 이 값들을 묶어 이벤트를 처리할 뿐 따로 상태를 갖지 않습니다. 검색 결과는 Soil, 즐겨찾기는 Room이 소유합니다.
 */
@Stable
internal class SearchStore(
    private val saved: MutableState<SearchSavedState>,
    private val favorites: FavoriteToggler,
    private val queryHolder: SearchQueryHolder,
) {
    val state: SearchSavedState by saved
    val pendingFavoriteIds: Set<String> get() = favorites.pendingIds
    val hasFavoriteFailure: Boolean get() = favorites.hasFailure

    fun holdQuery(client: QueryClient, key: InfiniteQueryKey<*, *>) = queryHolder.hold(client, key)

    fun releaseQuery() = queryHolder.release()

    fun dispatch(event: SearchEvent, server: SearchServer) {
        Timber.d("검색 이벤트: %s", event)
        when (event) {
            is SearchEvent.InputChanged -> update { copy(input = event.value) }
            SearchEvent.Submit -> {
                val query = state.input.trim()
                // 같은 조건을 다시 확정하면 새로 요청하지 않고 오류일 때만 재시도합니다.
                if (query == state.submittedQuery) server.retry() else update { copy(submittedQuery = query) }
            }
            is SearchEvent.SortSelected -> update { copy(sort = event.sort) }
            SearchEvent.LoadMore -> server.loadMore()
            SearchEvent.Retry -> server.retry()
            is SearchEvent.ToggleFavorite ->
                favorites.setFavorite(book = event.book, favorite = event.book.id !in server.favoriteIds)
            SearchEvent.UserMessageShown -> favorites.consumeFailure()
        }
    }

    private inline fun update(transform: SearchSavedState.() -> SearchSavedState) {
        saved.value = saved.value.transform()
    }
}

@Composable
internal fun rememberSearchStore(favoriteMutationKey: MutationKey<Unit, FavoriteCommand>): SearchStore {
    val saved = rememberSerializable { mutableStateOf(SearchSavedState()) }
    val favorites = retainFavoriteToggler(favoriteMutationKey)
    val queryHolder = retain { RetainedSearchQuery() }.holder
    return remember(saved, favorites, queryHolder) { SearchStore(saved, favorites, queryHolder) }
}

/**
 * 검색 루트가 백스택에 있는 동안 확정 조건의 쿼리를 Soil에 붙여 둡니다.
 *
 * Soil은 구독이 모두 끊기고 keepAliveTime(기본 5초)이 지나면 쿼리를 내리면서 성공 상태만 캐시에 남깁니다.
 * 즐겨찾기 탭으로 가면 검색 루트는 컴포지션에서 빠지므로, 붙잡지 않으면 추가 로딩이 실패한 목록이
 * 돌아왔을 때 지워져 1페이지부터 다시 받게 됩니다. 조건이 바뀌거나 검색어를 비우거나 엔트리가 은퇴하면 놓고,
 * 놓은 쿼리는 Soil의 기본 정책대로 정리됩니다. 은퇴 콜백은 어느 스레드에서든 불릴 수 있어 동기화합니다.
 */
internal class SearchQueryHolder {
    private var ref: AutoCloseable? = null
    private var id: InfiniteQueryId<*, *>? = null

    @Synchronized
    fun hold(client: QueryClient, key: InfiniteQueryKey<*, *>) {
        if (key.id == id) return
        release()
        ref = client.getInfiniteQuery(key)
        id = key.id
    }

    @Synchronized
    fun release() {
        ref?.close()
        ref = null
        id = null
    }
}

/** 검색 엔트리가 은퇴하면 붙잡아 둔 쿼리를 놓습니다. */
private class RetainedSearchQuery : RetainObserver {
    val holder = SearchQueryHolder()

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() = holder.release()

    override fun onUnused() = holder.release()
}
