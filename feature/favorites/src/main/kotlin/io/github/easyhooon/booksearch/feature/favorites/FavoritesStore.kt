package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.FavoriteSort
import io.github.easyhooon.booksearch.core.model.PriceFilter
import io.github.easyhooon.booksearch.core.ui.FavoriteToggler
import io.github.easyhooon.booksearch.core.ui.retainFavoriteToggler
import kotlinx.serialization.Serializable
import soil.query.MutationKey
import timber.log.Timber

/** 프로세스 재생성 뒤에도 복원하는 즐겨찾기 화면의 클라이언트 상태입니다. */
@Serializable
internal data class FavoritesSavedState(
    val input: String = "",
    val submittedQuery: String = "",
    val sort: FavoriteSort = FavoriteSort.TitleAscending,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
) {
    val priceFilter: PriceFilter get() = PriceFilter(min = minPrice, max = maxPrice)
}

/**
 * 즐겨찾기 화면의 클라이언트 상태를 한곳에서 바꿉니다.
 *
 * 저장 데이터는 [rememberSerializable], 진행 중인 즐겨찾기 쓰기는 retain으로 두고,
 * Store 자체는 이 값들을 묶어 이벤트를 처리할 뿐 따로 상태를 갖지 않습니다. 즐겨찾기 목록은 Room이 소유합니다.
 */
@Stable
internal class FavoritesStore(
    private val saved: MutableState<FavoritesSavedState>,
    private val favorites: FavoriteToggler,
) {
    val state: FavoritesSavedState by saved
    val pendingFavoriteIds: Set<String> get() = favorites.pendingIds
    val hasFavoriteFailure: Boolean get() = favorites.hasFailure

    /** [favoriteIds]는 Root가 Room에서 읽어 넘기는 값이라 보관하지 않고 호출마다 받습니다. */
    fun dispatch(event: FavoritesEvent, favoriteIds: Set<String>) {
        Timber.d("즐겨찾기 이벤트: %s", event)
        when (event) {
            is FavoritesEvent.InputChanged -> update { copy(input = event.value) }
            FavoritesEvent.Submit -> update { copy(submittedQuery = input.trim()) }
            is FavoritesEvent.SortSelected -> update { copy(sort = event.sort) }
            // 최소가 최대보다 큰 범위는 적용하지 않습니다. 화면도 적용 버튼을 막습니다.
            is FavoritesEvent.PriceFilterApplied -> if (event.filter.isValid) {
                update { copy(minPrice = event.filter.min, maxPrice = event.filter.max) }
            }
            FavoritesEvent.PriceFilterCleared -> update { copy(minPrice = null, maxPrice = null) }
            is FavoritesEvent.ToggleFavorite ->
                favorites.setFavorite(book = event.book, favorite = event.book.id !in favoriteIds)
            FavoritesEvent.UserMessageShown -> favorites.consumeFailure()
        }
    }

    private inline fun update(transform: FavoritesSavedState.() -> FavoritesSavedState) {
        saved.value = saved.value.transform()
    }
}

@Composable
internal fun rememberFavoritesStore(favoriteMutationKey: MutationKey<Unit, FavoriteCommand>): FavoritesStore {
    val saved = rememberSerializable { mutableStateOf(FavoritesSavedState()) }
    val favorites = retainFavoriteToggler(favoriteMutationKey)
    return remember(saved, favorites) { FavoritesStore(saved, favorites) }
}
