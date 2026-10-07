package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import soil.query.InfiniteQueryId
import soil.query.InfiniteQueryKey
import soil.query.QueryClient

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

/** 현재 엔트리 범위로 [SearchQueryHolder]를 유지하고, 엔트리가 은퇴하면 붙잡아 둔 쿼리를 놓습니다. */
@Composable
internal fun retainSearchQueryHolder(): SearchQueryHolder = retain { RetainedSearchQuery() }.holder

private class RetainedSearchQuery : RetainObserver {
    val holder = SearchQueryHolder()

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() = holder.release()

    override fun onUnused() = holder.release()
}
