package io.github.easyhooon.booksearch.feature.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import io.github.easyhooon.booksearch.core.ui.retainFavoriteToggler
import soil.query.MutationKey
import timber.log.Timber

@Immutable
data class BookDetailUiState(
    val book: Book,
    val isFavorite: Boolean,
    /** Room 값을 받기 전이거나 저장이 진행 중이면 false입니다. */
    val isFavoriteEnabled: Boolean,
    val userMessage: ErrorMessage?,
    val eventSink: (BookDetailEvent) -> Unit,
)

sealed interface BookDetailEvent {
    data object ToggleFavorite : BookDetailEvent
    data object UserMessageShown : BookDetailEvent
}

/**
 * Room에 같은 ID가 있으면 Room 값을, 없으면 NavKey의 스냅샷을 보여 줍니다.
 * 즐겨찾기 여부는 항상 Room 기준이며, 해제해서 Room 행이 사라져도 스냅샷으로 같은 정보를 유지합니다.
 *
 * [isRoomLoaded]가 false인 동안은 즐겨찾기 여부를 모르므로 하트를 잠급니다.
 */
@Composable
fun bookDetailPresenter(
    snapshot: Book,
    roomBook: Book?,
    isRoomLoaded: Boolean,
    favoriteMutationKey: MutationKey<Unit, FavoriteCommand>,
): BookDetailUiState {
    // 저장할 클라이언트 상태가 없어 Store 없이 엔트리 범위의 쓰기 진행 상태만 둡니다.
    val favoriteToggler = retainFavoriteToggler(favoriteMutationKey)
    val book = roomBook ?: snapshot
    val isFavorite = roomBook != null

    return BookDetailUiState(
        book = book,
        isFavorite = isFavorite,
        isFavoriteEnabled = isRoomLoaded && book.id !in favoriteToggler.pendingIds,
        userMessage = if (favoriteToggler.hasFailure) ErrorMessage.FavoriteWrite else null,
    ) { event ->
        Timber.d("상세 이벤트: %s", event)
        when (event) {
            BookDetailEvent.ToggleFavorite -> if (isRoomLoaded) {
                favoriteToggler.setFavorite(book = book, favorite = !isFavorite)
            }
            BookDetailEvent.UserMessageShown -> favoriteToggler.consumeFailure()
        }
    }
}
