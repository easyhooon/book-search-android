package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.database.FavoriteBookDao
import io.github.easyhooon.booksearch.core.database.FavoriteBookEntity
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FavoriteWriterTest {

    private class FakeDao : FavoriteBookDao {
        val rows = MutableStateFlow<Map<String, FavoriteBookEntity>>(emptyMap())
        val log = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        var failure: Throwable? = null

        override fun observeAll(): Flow<List<FavoriteBookEntity>> = rows.map { it.values.toList() }
        override fun observeIds(): Flow<List<String>> = rows.map { it.keys.toList() }
        override fun observeById(id: String): Flow<FavoriteBookEntity?> = rows.map { it[id] }

        override suspend fun upsert(entity: FavoriteBookEntity) {
            gate?.await()
            failure?.let { throw it }
            log += "upsert:${entity.id}"
            rows.value = rows.value + (entity.id to entity)
        }

        override suspend fun deleteById(id: String) {
            gate?.await()
            failure?.let { throw it }
            log += "delete:$id"
            rows.value = rows.value - id
        }
    }

    private fun book(id: String) = Book(
        id = id, isbn10 = null, isbn13 = null, title = id, authors = emptyList(), translators = emptyList(),
        publisher = "", publishedDate = null, price = null, salePrice = null, thumbnailUrl = "",
        contents = "", url = "", status = "",
    )

    @Test
    fun `호출한 화면의 코루틴이 취소되어도 쓰기는 끝까지 진행한다`() = runTest {
        val dao = FakeDao().apply { gate = CompletableDeferred() }
        val appScope = ApplicationCoroutineScope(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))
        val writer = FavoriteWriter(dao, appScope)

        val screenJob = launch { writer.write(FavoriteCommand(book("a"), favorite = true)) }
        advanceUntilIdle()
        screenJob.cancel()
        dao.gate!!.complete(Unit)
        advanceUntilIdle()

        assertTrue(screenJob.isCancelled)
        assertEquals(setOf("a"), dao.rows.value.keys)
    }

    @Test
    fun `쓰기는 요청 순서대로 직렬 실행된다`() = runTest {
        val dao = FakeDao()
        val appScope = ApplicationCoroutineScope(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))
        val writer = FavoriteWriter(dao, appScope)

        launch { writer.write(FavoriteCommand(book("a"), favorite = true)) }
        launch { writer.write(FavoriteCommand(book("a"), favorite = false)) }
        launch { writer.write(FavoriteCommand(book("a"), favorite = true)) }
        advanceUntilIdle()

        assertEquals(listOf("upsert:a", "delete:a", "upsert:a"), dao.log)
        assertEquals(setOf("a"), dao.rows.value.keys)
    }

    @Test
    fun `쓰기 실패는 호출자에게 예외로 전달하고 다음 쓰기를 막지 않는다`() = runTest {
        val dao = FakeDao().apply { failure = IllegalStateException("disk full") }
        val appScope = ApplicationCoroutineScope(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))
        val writer = FavoriteWriter(dao, appScope)

        try {
            writer.write(FavoriteCommand(book("a"), favorite = true))
            fail("예외가 발생해야 합니다")
        } catch (expected: IllegalStateException) {
            assertEquals("disk full", expected.message)
        }

        dao.failure = null
        writer.write(FavoriteCommand(book("b"), favorite = true))
        assertEquals(setOf("b"), dao.rows.value.keys)
    }
}
