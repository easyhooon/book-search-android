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
import soil.query.MutationRef
import soil.query.compose.LocalMutationClient
import timber.log.Timber

/**
 * 화면 하나의 즐겨찾기 쓰기 진행 상태입니다.
 *
 * [retainFavoriteToggler]가 엔트리 범위로 유지하므로 회전이나 상세 진입처럼 컴포지션만 사라질 때는
 * 진행 중인 ID와 실패 알림이 유지되고, 엔트리가 pop되면 [scope]가 취소되어 대기만 멈춥니다.
 * 이미 시작한 DB 쓰기는 앱 범위의 FavoriteWriter에서 끝까지 실행됩니다.
 */
@Stable
class FavoriteToggler(
    private val scope: CoroutineScope,
    private val mutate: suspend (FavoriteCommand) -> Unit,
) {
    private val pending = mutableStateSetOf<String>()

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
}

/**
 * 현재 엔트리 범위로 [FavoriteToggler]를 유지합니다.
 *
 * retain 값이 컴포지션 범위인 `rememberMutation`을 참조하지 않도록 Mutation 구독을 엔트리 범위로 직접 잡고,
 * 엔트리가 은퇴할 때 놓습니다.
 */
@Composable
fun retainFavoriteToggler(key: MutationKey<Unit, FavoriteCommand>): FavoriteToggler {
    val client = LocalMutationClient.current
    return retain(client, key.id) { RetainedFavoriteToggler(client.getMutation(key)) }.toggler
}

private class RetainedFavoriteToggler(private val mutation: MutationRef<Unit, FavoriteCommand>) : RetainObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val toggler = FavoriteToggler(scope, mutation::mutate)

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() = close()

    override fun onUnused() = close()

    private fun close() {
        scope.cancel()
        mutation.close()
    }
}
