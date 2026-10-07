package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.FavoriteListRules
import io.github.easyhooon.booksearch.core.model.FavoriteSort
import io.github.easyhooon.booksearch.core.model.PriceFilter
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import soil.query.MutationKey

@Immutable
data class FavoritesUiState(
    val input: String,
    val submittedQuery: String,
    val sort: FavoriteSort,
    val priceFilter: PriceFilter,
    val content: FavoritesContent,
    val pendingFavoriteIds: Set<String>,
    val userMessage: ErrorMessage?,
    val eventSink: (FavoritesEvent) -> Unit,
)

sealed interface FavoritesContent {
    data object Loading : FavoritesContent

    /** 즐겨찾기가 하나도 없습니다. */
    data object NoFavorites : FavoritesContent

    /** 즐겨찾기는 있지만 검색어나 가격 조건에 맞는 도서가 없습니다. */
    data object NoMatch : FavoritesContent

    data class Results(val books: List<Book>) : FavoritesContent
}

sealed interface FavoritesEvent {
    data class InputChanged(val value: String) : FavoritesEvent
    data object Submit : FavoritesEvent
    data class SortSelected(val sort: FavoriteSort) : FavoritesEvent
    data class PriceFilterApplied(val filter: PriceFilter) : FavoritesEvent
    data object PriceFilterCleared : FavoritesEvent
    data class ToggleFavorite(val book: Book) : FavoritesEvent
    data object UserMessageShown : FavoritesEvent
}

/**
 * Room에서 읽은 전체 즐겨찾기에 [FavoritesStore]의 확정 검색어·정렬·가격 필터를 메모리에서 적용합니다.
 *
 * [favoriteBooks]가 null이면 아직 Room 값을 받지 못한 상태입니다.
 * 하트를 눌러 해제하면 Room 반영과 함께 목록에서 사라집니다.
 */
@Composable
fun favoritesPresenter(
    favoriteBooks: List<Book>?,
    favoriteMutationKey: MutationKey<Unit, FavoriteCommand>,
): FavoritesUiState {
    val store = rememberFavoritesStore(favoriteMutationKey)
    val state = store.state

    val content = remember(favoriteBooks, state.submittedQuery, state.sort, state.priceFilter) {
        when {
            favoriteBooks == null -> FavoritesContent.Loading
            favoriteBooks.isEmpty() -> FavoritesContent.NoFavorites
            else -> FavoriteListRules.apply(favoriteBooks, state.submittedQuery, state.sort, state.priceFilter)
                .let { if (it.isEmpty()) FavoritesContent.NoMatch else FavoritesContent.Results(it) }
        }
    }
    val favoriteIds = remember(favoriteBooks) { favoriteBooks.orEmpty().mapTo(HashSet()) { it.id } }

    return FavoritesUiState(
        input = state.input,
        submittedQuery = state.submittedQuery,
        sort = state.sort,
        priceFilter = state.priceFilter,
        content = content,
        pendingFavoriteIds = store.pendingFavoriteIds.toSet(),
        userMessage = if (store.hasFavoriteFailure) ErrorMessage.FavoriteWrite else null,
        eventSink = { event -> store.dispatch(event, favoriteIds) },
    )
}
