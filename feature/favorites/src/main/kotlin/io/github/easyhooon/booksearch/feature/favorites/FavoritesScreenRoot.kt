package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.easyhooon.booksearch.core.model.Book
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.compose.rememberSubscription

/**
 * 즐겨찾기 탭 엔트리입니다. 상세 이동처럼 화면 밖 동작은 app이 연결합니다.
 * [metadata]는 2-pane 배치 역할을, [selectedBookId]는 2-pane에서 오른쪽에 열린 도서를 app이 알려 줍니다.
 */
fun EntryProviderScope<NavKey>.favoritesEntry(
    graphFactory: FavoritesScreenGraph.Factory,
    onBookClick: (Book) -> Unit,
    metadata: Map<String, Any> = emptyMap(),
    selectedBookId: () -> String? = { null },
) {
    entry<FavoritesNavKey>(metadata = metadata) {
        val graph = retain { graphFactory.createFavoritesScreenGraph() }
        FavoritesScreenRoot(context = graph.favoritesScreenContext, onBookClick = onBookClick, selectedBookId = selectedBookId())
    }
}

@OptIn(ExperimentalSoilQueryApi::class)
@Composable
fun FavoritesScreenRoot(
    context: FavoritesScreenContext,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    selectedBookId: String? = null,
) {
    val favoriteBooks = rememberSubscription(context.favoriteBooksSubscriptionKey)
    val uiState = favoritesPresenter(
        favoriteBooks = favoriteBooks.data,
        favoriteMutationKey = context.favoriteMutationKey,
    )
    FavoritesScreen(uiState = uiState, onBookClick = onBookClick, modifier = modifier, selectedBookId = selectedBookId)
}
