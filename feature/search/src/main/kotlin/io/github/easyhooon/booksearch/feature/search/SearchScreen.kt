package io.github.easyhooon.booksearch.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.ui.component.BookCard
import io.github.easyhooon.booksearch.core.ui.component.BookListDefaults
import io.github.easyhooon.booksearch.core.ui.component.ListFooter
import io.github.easyhooon.booksearch.core.ui.component.LoadingContent
import io.github.easyhooon.booksearch.core.ui.component.MessageContent
import io.github.easyhooon.booksearch.core.ui.component.QueryField
import io.github.easyhooon.booksearch.core.ui.component.SortMenuButton
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage = uiState.userMessage?.let { stringResource(it.resId) }
    val eventSink by rememberUpdatedState(uiState.eventSink)
    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage)
            eventSink(SearchEvent.UserMessageShown)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = BookListDefaults.QueryBarMaxWidth)
                    .padding(horizontal = 16.dp),
            ) {
                QueryField(
                    value = uiState.input,
                    placeholder = stringResource(R.string.search_hint),
                    onValueChange = { uiState.eventSink(SearchEvent.InputChanged(it)) },
                    onSubmit = { uiState.eventSink(SearchEvent.Submit) },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag(SearchTestTags.SEARCH_FIELD),
                )
                SortMenuButton(
                    selected = uiState.sort,
                    options = SearchSort.entries,
                    label = { stringResource(it.labelRes) },
                    onSelect = { uiState.eventSink(SearchEvent.SortSelected(it)) },
                    modifier = Modifier.align(Alignment.End),
                )
            }
            SearchContentArea(
                uiState = uiState,
                onBookClick = onBookClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchContentArea(
    uiState: SearchUiState,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onRetry = { uiState.eventSink(SearchEvent.Retry) }
    when (val content = uiState.content) {
        SearchContent.Idle -> MessageContent(stringResource(R.string.search_idle), modifier)
        SearchContent.Loading -> LoadingContent(modifier)
        SearchContent.Empty -> MessageContent(stringResource(R.string.search_empty), modifier)
        is SearchContent.Error -> MessageContent(stringResource(content.message.resId), modifier, onRetry)
        is SearchContent.Results -> {
            // 조건이 바뀌면 새 목록이므로 맨 위에서 시작합니다.
            val gridState = rememberSaveable(uiState.submittedQuery, uiState.sort, saver = LazyGridState.Saver) {
                LazyGridState()
            }
            LoadMoreEffect(
                gridState = gridState,
                itemCount = content.books.size,
                onLoadMore = { uiState.eventSink(SearchEvent.LoadMore) },
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(BookListDefaults.GridMinCellWidth),
                state = gridState,
                modifier = modifier.testTag(SearchTestTags.RESULT_GRID),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(BookListDefaults.GridItemSpacing),
                horizontalArrangement = Arrangement.spacedBy(BookListDefaults.GridItemSpacing),
            ) {
                items(content.books, key = { it.id }) { book ->
                    BookCard(
                        book = book,
                        isFavorite = book.id in uiState.favoriteIds,
                        isFavoritePending = book.id in uiState.pendingFavoriteIds,
                        onClick = { onBookClick(book) },
                        onFavoriteClick = { uiState.eventSink(SearchEvent.ToggleFavorite(book)) },
                    )
                }
                if (content.isLoadingMore || content.loadMoreError != null) {
                    item(key = FOOTER_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        ListFooter(
                            isLoading = content.isLoadingMore,
                            errorMessage = content.loadMoreError?.let { stringResource(it.resId) },
                            onRetry = onRetry,
                        )
                    }
                }
            }
        }
    }
}

/** 마지막 아이템 근처가 보이면 다음 페이지를 요청합니다. 중복 요청은 Presenter와 Soil이 거릅니다. */
@Composable
private fun LoadMoreEffect(
    gridState: LazyGridState,
    itemCount: Int,
    onLoadMore: () -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(gridState, itemCount) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (SearchPaging.shouldLoadMore(lastVisibleIndex, itemCount)) currentOnLoadMore()
            }
    }
}

private const val FOOTER_KEY = "search-footer"

private val SearchSort.labelRes: Int
    get() = when (this) {
        SearchSort.Accuracy -> R.string.sort_accuracy
        SearchSort.Latest -> R.string.sort_latest
    }

object SearchTestTags {
    const val SEARCH_FIELD = "search-field"
    const val RESULT_GRID = "search-result-grid"
}
