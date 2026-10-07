package io.github.easyhooon.booksearch.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.ui.component.BookCard
import io.github.easyhooon.booksearch.core.ui.component.ListFooter
import io.github.easyhooon.booksearch.core.ui.component.LoadingContent
import io.github.easyhooon.booksearch.core.ui.component.MessageContent
import kotlinx.coroutines.flow.distinctUntilChanged

private val SearchBarMaxWidth = 640.dp
private val GridMinCellWidth = 360.dp

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
                    .widthIn(max = SearchBarMaxWidth)
                    .padding(horizontal = 16.dp),
            ) {
                SearchField(
                    value = uiState.input,
                    onValueChange = { uiState.eventSink(SearchEvent.InputChanged(it)) },
                    onSubmit = { uiState.eventSink(SearchEvent.Submit) },
                )
                SortRow(
                    sort = uiState.sort,
                    onSortSelected = { uiState.eventSink(SearchEvent.SortSelected(it)) },
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
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val submit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSubmit()
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag(SearchTestTags.SEARCH_FIELD),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                    }
                }
                IconButton(onClick = submit) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_action))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
    )
}

@Composable
private fun SortRow(
    sort: SearchSort,
    onSortSelected: (SearchSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(stringResource(sort.labelRes))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.sort_change))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SearchSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            expanded = false
                            onSortSelected(option)
                        },
                    )
                }
            }
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
                columns = GridCells.Adaptive(GridMinCellWidth),
                state = gridState,
                modifier = modifier.testTag(SearchTestTags.RESULT_GRID),
                contentPadding = PaddingValues(16.dp),
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
