package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.database.FavoriteBookDao
import io.github.easyhooon.booksearch.core.database.toFavoriteEntity
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 즐겨찾기 쓰기를 앱 범위에서 직렬로 실행합니다.
 *
 * 호출한 화면이 사라져 [write]를 기다리던 코루틴이 취소되어도 Room 쓰기는 끝까지 진행합니다.
 * 결과와 예외는 호출자가 기다리는 동안에는 그대로 전달하므로 Soil Mutation의 상태와 일치합니다.
 */
class FavoriteWriter(
    private val dao: FavoriteBookDao,
    private val scope: ApplicationCoroutineScope,
) {
    private val mutex = Mutex()

    suspend fun write(command: FavoriteCommand) {
        scope.async {
            mutex.withLock {
                if (command.favorite) {
                    dao.upsert(command.book.toFavoriteEntity(savedAt = System.currentTimeMillis()))
                } else {
                    dao.deleteById(command.book.id)
                }
            }
        }.await()
    }
}
