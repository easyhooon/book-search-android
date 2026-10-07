package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.easyhooon.booksearch.core.model.Book
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.compose.rememberSubscription

/**
 * 검색 탭 엔트리입니다. 상세 이동처럼 화면 밖 동작은 app이 연결합니다.
 * [metadata]는 2-pane 배치 역할을, [selectedBookId]는 2-pane에서 오른쪽에 열린 도서를 app이 알려 줍니다.
 */
fun EntryProviderScope<NavKey>.searchEntry(
    graphFactory: SearchScreenGraph.Factory,
    onBookClick: (Book) -> Unit,
    metadata: Map<String, Any> = emptyMap(),
    selectedBookId: () -> String? = { null },
) {
    entry<SearchNavKey>(metadata = metadata) {
        val graph = retain { graphFactory.createSearchScreenGraph() }
        SearchScreenRoot(context = graph.searchScreenContext, onBookClick = onBookClick, selectedBookId = selectedBookId())
    }
}

@OptIn(ExperimentalSoilQueryApi::class)
@Composable
fun SearchScreenRoot(
    context: SearchScreenContext,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    selectedBookId: String? = null,
) {
    // Room을 읽는 동안에는 빈 집합으로 그립니다. 하트는 Room 값이 오면 바로 맞춰집니다.
    val favoriteIds = rememberSubscription(context.favoriteIdsSubscriptionKey)
    val uiState = searchPresenter(
        queryKeyFactory = context.searchBooksQueryKeyFactory,
        favoriteIds = favoriteIds.data.orEmpty(),
        favoriteMutationKey = context.favoriteMutationKey,
    )
    SearchScreen(uiState = uiState, onBookClick = onBookClick, modifier = modifier, selectedBookId = selectedBookId)
}
