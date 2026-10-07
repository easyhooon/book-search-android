package io.github.easyhooon.booksearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.feature.detail.BookDetailScreenGraph
import io.github.easyhooon.booksearch.feature.detail.bookDetailEntry
import io.github.easyhooon.booksearch.feature.favorites.FavoritesScreenGraph
import io.github.easyhooon.booksearch.feature.favorites.favoritesEntry
import io.github.easyhooon.booksearch.feature.search.SearchNavKey
import io.github.easyhooon.booksearch.feature.search.SearchScreenGraph
import io.github.easyhooon.booksearch.feature.search.searchEntry
import io.github.easyhooon.booksearch.navigation.BookSearchNavHost
import io.github.easyhooon.booksearch.navigation.ListDetailMetadata
import io.github.easyhooon.booksearch.navigation.isListDetailTwoPane
import io.github.easyhooon.booksearch.navigation.navigateBack
import io.github.easyhooon.booksearch.navigation.openDetail
import io.github.easyhooon.booksearch.navigation.openedBookId

@Composable
fun BookSearchApp(
    appGraph: AppGraph,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(SearchNavKey)
    val isTwoPane = isListDetailTwoPane()
    // 엔트리는 백스택이 바뀔 때만 다시 만들어지므로, 창 크기는 람다 안에서 최신 값을 읽습니다.
    val currentIsTwoPane by rememberUpdatedState(isTwoPane)
    val onBookClick = { book: Book -> backStack.openDetail(book, replaceDetail = currentIsTwoPane) }
    val selectedBookId = { if (currentIsTwoPane) backStack.openedBookId else null }
    BookSearchNavHost(
        backStack = backStack,
        entryProvider = entryProvider<NavKey> {
            searchEntry(
                graphFactory = appGraph as SearchScreenGraph.Factory,
                onBookClick = onBookClick,
                metadata = ListDetailMetadata.list,
                selectedBookId = selectedBookId,
            )
            favoritesEntry(
                graphFactory = appGraph as FavoritesScreenGraph.Factory,
                onBookClick = onBookClick,
                metadata = ListDetailMetadata.list,
                selectedBookId = selectedBookId,
            )
            bookDetailEntry(
                graphFactory = appGraph as BookDetailScreenGraph.Factory,
                onBack = { backStack.navigateBack() },
                metadata = ListDetailMetadata.detail,
            )
        },
        modifier = modifier,
        isTwoPane = isTwoPane,
    )
}
