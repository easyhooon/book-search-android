package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.easyhooon.booksearch.core.model.Book
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.compose.rememberSubscription

/** 즐겨찾기 탭 엔트리입니다. 상세 이동처럼 화면 밖 동작은 app이 연결합니다. */
fun EntryProviderScope<NavKey>.favoritesEntry(
    graphFactory: FavoritesScreenGraph.Factory,
    onBookClick: (Book) -> Unit,
) {
    entry<FavoritesNavKey> {
        val graph = retain { graphFactory.createFavoritesScreenGraph() }
        FavoritesScreenRoot(context = graph.favoritesScreenContext, onBookClick = onBookClick)
    }
}

@OptIn(ExperimentalSoilQueryApi::class)
@Composable
fun FavoritesScreenRoot(
    context: FavoritesScreenContext,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteBooks = rememberSubscription(context.favoriteBooksSubscriptionKey)
    val uiState = favoritesPresenter(
        favoriteBooks = favoriteBooks.data,
        favoriteMutationKey = context.favoriteMutationKey,
    )
    FavoritesScreen(uiState = uiState, onBookClick = onBookClick, modifier = modifier)
}
