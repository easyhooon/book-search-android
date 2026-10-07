package io.github.easyhooon.booksearch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import io.github.easyhooon.booksearch.feature.detail.BookDetailScreenGraph
import io.github.easyhooon.booksearch.feature.detail.bookDetailEntry
import io.github.easyhooon.booksearch.feature.favorites.FavoritesScreenGraph
import io.github.easyhooon.booksearch.feature.favorites.favoritesEntry
import io.github.easyhooon.booksearch.feature.search.SearchNavKey
import io.github.easyhooon.booksearch.feature.search.SearchScreenGraph
import io.github.easyhooon.booksearch.feature.search.searchEntry
import io.github.easyhooon.booksearch.navigation.BookSearchNavHost
import io.github.easyhooon.booksearch.navigation.navigateBack
import io.github.easyhooon.booksearch.navigation.openDetail

@Composable
fun BookSearchApp(
    appGraph: AppGraph,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(SearchNavKey)
    BookSearchNavHost(
        backStack = backStack,
        entryProvider = entryProvider<NavKey> {
            searchEntry(graphFactory = appGraph as SearchScreenGraph.Factory, onBookClick = backStack::openDetail)
            favoritesEntry(graphFactory = appGraph as FavoritesScreenGraph.Factory, onBookClick = backStack::openDetail)
            bookDetailEntry(graphFactory = appGraph as BookDetailScreenGraph.Factory, onBack = { backStack.navigateBack() })
        },
        modifier = modifier,
    )
}
