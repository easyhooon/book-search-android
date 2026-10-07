package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.FavoriteListRules
import io.github.easyhooon.booksearch.core.model.FavoriteSort
import io.github.easyhooon.booksearch.core.model.PriceFilter
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import io.github.easyhooon.booksearch.core.ui.retainFavoriteToggler
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
 * Room에서 읽은 전체 즐겨찾기에 확정 검색어·정렬·가격 필터를 메모리에서 적용합니다.
 *
 * [favoriteBooks]가 null이면 아직 Room 값을 받지 못한 상태입니다.
 * 하트를 눌러 해제하면 Room 반영과 함께 목록에서 사라집니다.
 */
@Composable
fun favoritesPresenter(
    favoriteBooks: List<Book>?,
    favoriteMutationKey: MutationKey<Unit, FavoriteCommand>,
): FavoritesUiState {
    var input by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(FavoriteSort.TitleAscending) }
    var minPrice by rememberSaveable { mutableStateOf<Int?>(null) }
    var maxPrice by rememberSaveable { mutableStateOf<Int?>(null) }
    val priceFilter = PriceFilter(min = minPrice, max = maxPrice)
    val favoriteToggler = retainFavoriteToggler(favoriteMutationKey)

    val content = remember(favoriteBooks, submittedQuery, sort, priceFilter) {
        when {
            favoriteBooks == null -> FavoritesContent.Loading
            favoriteBooks.isEmpty() -> FavoritesContent.NoFavorites
            else -> FavoriteListRules.apply(favoriteBooks, submittedQuery, sort, priceFilter)
                .let { if (it.isEmpty()) FavoritesContent.NoMatch else FavoritesContent.Results(it) }
        }
    }
    val favoriteIds = remember(favoriteBooks) { favoriteBooks.orEmpty().mapTo(HashSet()) { it.id } }

    return FavoritesUiState(
        input = input,
        submittedQuery = submittedQuery,
        sort = sort,
        priceFilter = priceFilter,
        content = content,
        pendingFavoriteIds = favoriteToggler.pendingIds.toSet(),
        userMessage = if (favoriteToggler.hasFailure) ErrorMessage.FavoriteWrite else null,
    ) { event ->
        when (event) {
            is FavoritesEvent.InputChanged -> input = event.value
            FavoritesEvent.Submit -> submittedQuery = input.trim()
            is FavoritesEvent.SortSelected -> sort = event.sort
            is FavoritesEvent.PriceFilterApplied -> {
                // 최소가 최대보다 큰 범위는 적용하지 않습니다. 화면도 적용 버튼을 막습니다.
                if (event.filter.isValid) {
                    minPrice = event.filter.min
                    maxPrice = event.filter.max
                }
            }
            FavoritesEvent.PriceFilterCleared -> {
                minPrice = null
                maxPrice = null
            }
            is FavoritesEvent.ToggleFavorite -> favoriteToggler.setFavorite(
                book = event.book,
                favorite = event.book.id !in favoriteIds,
            )
            FavoritesEvent.UserMessageShown -> favoriteToggler.consumeFailure()
        }
    }
}
