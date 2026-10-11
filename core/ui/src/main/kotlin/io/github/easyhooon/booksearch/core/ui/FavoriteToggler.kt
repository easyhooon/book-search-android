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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import soil.query.MutationKey
import soil.query.MutationRef
import soil.query.compose.LocalMutationClient
import timber.log.Timber

/**
 * 화면 하나의 즐겨찾기 쓰기 진행 상태입니다.
 *
 * [retainFavoriteToggler]가 엔트리 범위로 유지하므로 회전이나 상세 진입처럼 컴포지션만 사라질 때는
 * 진행 중인 ID와 실패 알림이 유지됩니다. 엔트리가 pop되면 신규 요청을 막고 이미 접수한 명령은
 * Mutation 전달과 DB 쓰기 완료까지 기다린 뒤 요청 scope와 구독을 정리합니다.
 * DB 쓰기는 공통 Mutation actor에서 직접 수행하며 화면이 닫힌 뒤 실패해도 로그를 남깁니다.
 */
@Stable
class FavoriteToggler(
    private val scope: CoroutineScope,
    private val mutate: suspend (FavoriteCommand) -> Unit,
) {
    private val pending = mutableStateSetOf<String>()
    private var acceptingRequests = true

    val pendingIds: Set<String> get() = pending

    /** 쓰기가 실패했으면 true입니다. 스낵바를 띄운 뒤 [consumeFailure]로 내립니다. */
    var hasFailure: Boolean by mutableStateOf(false)
        private set

    /** 같은 도서의 쓰기가 진행 중이면 무시합니다. 명령은 토글이 아니라 목표 상태입니다. */
    fun setFavorite(book: Book, favorite: Boolean) {
        if (!acceptingRequests || !scope.isActive || !pending.add(book.id)) return
        val command = FavoriteCommand(book, favorite)
        // Mutation 전송을 즉시 시작해 UI의 접수 순서를 유지합니다.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                mutate(command)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "즐겨찾기 쓰기 실패: id=%s, favorite=%s", book.id, favorite)
                if (acceptingRequests) hasFailure = true
            } finally {
                pending.remove(book.id)
            }
        }
    }

    internal fun stopAcceptingRequests() {
        acceptingRequests = false
    }

    fun consumeFailure() {
        hasFailure = false
    }
}

/**
 * 현재 엔트리 범위로 [FavoriteToggler]를 유지합니다.
 *
 * Mutation 구독을 직접 잡고, 엔트리가 은퇴해도 접수한 요청이 모두 완료될 때까지 유지합니다.
 * 화면별 진행 표시는 각 Toggler가 소유하고 Mutation actor는 모든 화면의 쓰기를 순서대로 실행합니다.
 */
@Composable
fun retainFavoriteToggler(key: MutationKey<Unit, FavoriteCommand>): FavoriteToggler {
    val client = LocalMutationClient.current
    return retain(client, key.id) { RetainedFavoriteToggler(client.getMutation(key)) }.toggler
}

private class RetainedFavoriteToggler(private val mutation: MutationRef<Unit, FavoriteCommand>) : RetainObserver {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    val toggler = FavoriteToggler(scope, mutation::mutate)

    init {
        // 마지막 요청이 완료된 메인 스레드에서 정리합니다. 그 전에는 actor의 keepAlive 종료를 막습니다.
        job.invokeOnCompletion { mutation.close() }
    }

    override fun onRetained() = Unit

    override fun onEnteredComposition() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() = close()

    override fun onUnused() = close()

    private fun close() {
        toggler.stopAcceptingRequests()
        job.complete()
    }
}
