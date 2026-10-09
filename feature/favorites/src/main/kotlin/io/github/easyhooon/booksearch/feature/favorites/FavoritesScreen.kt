package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteSort
import io.github.easyhooon.booksearch.core.model.PriceFilter
import io.github.easyhooon.booksearch.core.ui.BookFormatters
import io.github.easyhooon.booksearch.core.ui.component.BookCard
import io.github.easyhooon.booksearch.core.ui.component.BookListDefaults
import io.github.easyhooon.booksearch.core.ui.component.LoadingContent
import io.github.easyhooon.booksearch.core.ui.component.MessageContent
import io.github.easyhooon.booksearch.core.ui.component.QueryField
import io.github.easyhooon.booksearch.core.ui.component.SortMenuButton

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    selectedBookId: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage = uiState.userMessage?.let { stringResource(it.resId) }
    val eventSink by rememberUpdatedState(uiState.eventSink)
    var showPriceFilter by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage)
            eventSink(FavoritesEvent.UserMessageShown)
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
                    placeholder = stringResource(R.string.favorites_hint),
                    onValueChange = { uiState.eventSink(FavoritesEvent.InputChanged(it)) },
                    onSubmit = { uiState.eventSink(FavoritesEvent.Submit) },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag(FavoritesTestTags.QUERY_FIELD),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = uiState.priceFilter.isActive,
                        onClick = {
                            // 시트가 닫힐 때 검색창으로 포커스가 돌아가 키보드가 다시 뜨지 않도록 합니다.
                            focusManager.clearFocus()
                            showPriceFilter = true
                        },
                        label = { Text(uiState.priceFilter.label()) },
                        modifier = Modifier.testTag(FavoritesTestTags.PRICE_FILTER_CHIP),
                    )
                    Spacer(Modifier.weight(1f))
                    SortMenuButton(
                        selected = uiState.sort,
                        options = FavoriteSort.entries,
                        label = { stringResource(it.labelRes) },
                        onSelect = { uiState.eventSink(FavoritesEvent.SortSelected(it)) },
                    )
                }
            }
            FavoritesContentArea(
                uiState = uiState,
                selectedBookId = selectedBookId,
                onBookClick = onBookClick,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showPriceFilter) {
        PriceFilterSheet(
            current = uiState.priceFilter,
            onApply = { uiState.eventSink(FavoritesEvent.PriceFilterApplied(it)) },
            onClear = { uiState.eventSink(FavoritesEvent.PriceFilterCleared) },
            onDismiss = { showPriceFilter = false },
        )
    }
}

@Composable
private fun FavoritesContentArea(
    uiState: FavoritesUiState,
    selectedBookId: String?,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val content = uiState.content) {
        FavoritesContent.Loading -> LoadingContent(modifier)
        FavoritesContent.NoFavorites -> MessageContent(stringResource(R.string.favorites_empty), modifier)
        FavoritesContent.NoMatch -> MessageContent(stringResource(R.string.favorites_no_match), modifier)
        is FavoritesContent.Results -> {
            // 조회 조건이 바뀌면 새 목록이므로 맨 위에서 시작합니다. 하트 해제처럼 조건이 같으면 위치를 유지합니다.
            val gridState = rememberSaveable(
                uiState.submittedQuery,
                uiState.sort,
                uiState.priceFilter,
                saver = LazyGridState.Saver,
            ) {
                LazyGridState()
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(BookListDefaults.GridMinCellWidth),
                state = gridState,
                modifier = modifier.testTag(FavoritesTestTags.RESULT_GRID),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(BookListDefaults.GridItemSpacing),
                horizontalArrangement = Arrangement.spacedBy(BookListDefaults.GridItemSpacing),
            ) {
                items(content.books, key = { it.id }) { book ->
                    BookCard(
                        book = book,
                        isFavorite = true,
                        isFavoritePending = book.id in uiState.pendingFavoriteIds,
                        onClick = { onBookClick(book) },
                        onFavoriteClick = { uiState.eventSink(FavoritesEvent.ToggleFavorite(book)) },
                        selected = book.id == selectedBookId,
                    )
                }
            }
        }
    }
}

/** 필터가 적용 중이면 칩에 범위를 그대로 보여 줍니다. */
@Composable
private fun PriceFilter.label(): String {
    val min = min?.let(BookFormatters::price)
    val max = max?.let(BookFormatters::price)

    return when {
        min != null && max != null -> stringResource(R.string.price_filter_range, min, max)
        min != null -> stringResource(R.string.price_filter_at_least, min)
        max != null -> stringResource(R.string.price_filter_at_most, max)
        else -> stringResource(R.string.price_filter)
    }
}

private val FavoriteSort.labelRes: Int
    get() = when (this) {
        FavoriteSort.TitleAscending -> R.string.sort_title_ascending
        FavoriteSort.TitleDescending -> R.string.sort_title_descending
    }

object FavoritesTestTags {
    const val QUERY_FIELD = "favorites-query-field"
    const val PRICE_FILTER_CHIP = "favorites-price-filter-chip"
    const val PRICE_MIN_FIELD = "favorites-price-min"
    const val PRICE_MAX_FIELD = "favorites-price-max"
    const val RESULT_GRID = "favorites-result-grid"
}
