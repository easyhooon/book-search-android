package io.github.easyhooon.booksearch.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import soil.query.MutationKey
import soil.query.compose.rememberMutation
import timber.log.Timber

/**
 * 화면 하나의 즐겨찾기 쓰기 진행 상태입니다.
 *
 * 엔트리 범위로 retain되므로 회전이나 상세 진입처럼 컴포지션만 사라질 때는 진행 중인 ID와 실패
 * 알림이 유지되고, 엔트리가 pop되면 [onRetired]에서 대기만 취소합니다. 이미 시작한 DB 쓰기는
 * 앱 범위의 FavoriteWriter에서 끝까지 실행됩니다.
 */
@Stable
class FavoriteToggler internal constructor() : RetainObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pending = mutableStateSetOf<String>()

    internal var mutate: suspend (FavoriteCommand) -> Unit = {}

    val pendingIds: Set<String> get() = pending

    /** 쓰기가 실패했으면 true입니다. 스낵바를 띄운 뒤 [consumeFailure]로 내립니다. */
    var hasFailure: Boolean by mutableStateOf(false)
        private set

    /** 같은 도서의 쓰기가 진행 중이면 무시합니다. 명령은 토글이 아니라 목표 상태입니다. */
    fun setFavorite(book: Book, favorite: Boolean) {
        if (!pending.add(book.id)) return
        val command = FavoriteCommand(book, favorite)
        scope.launch {
            try {
                mutate(command)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "즐겨찾기 쓰기 실패: id=%s, favorite=%s", book.id, favorite)
                hasFailure = true
            } finally {
                pending.remove(book.id)
            }
        }
    }

    fun consumeFailure() {
        hasFailure = false
    }

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() = scope.cancel()

    override fun onUnused() = scope.cancel()
}

@Composable
fun retainFavoriteToggler(key: MutationKey<Unit, FavoriteCommand>): FavoriteToggler {
    val mutation = rememberMutation(key)
    val toggler = retain { FavoriteToggler() }
    toggler.mutate = mutation.mutate
    return toggler
}
