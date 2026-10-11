package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.database.FavoriteBookDao
import io.github.easyhooon.booksearch.core.database.FavoriteBookEntity
import io.github.easyhooon.booksearch.core.database.toBook
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import soil.query.MutationReceiver
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.core.Marker
import java.io.IOException

/** 실제 Mutation key·actor·DAO 경계에서 순서와 실패·취소를 확인합니다. */
@OptIn(ExperimentalSoilQueryApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoriteMutationKeyTest {
    private class DelayedDao : FavoriteBookDao {
        val rows = MutableStateFlow<Map<String, FavoriteBookEntity>>(emptyMap())
        val started = mutableListOf<String>()
        val writes = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        var failure: Throwable? = null
        var cancelled = false

        override fun observeAll(): Flow<List<FavoriteBookEntity>> = rows.map { it.values.toList() }
        override fun observeIds(): Flow<List<String>> = rows.map { it.keys.toList() }
        override fun observeById(id: String): Flow<FavoriteBookEntity?> = rows.map { it[id] }

        private suspend fun beforeWrite(operation: String) {
            started += operation
            try {
                gate.await()
                failure?.let { throw it }
            } catch (e: CancellationException) {
                cancelled = true
                throw e
            }
            writes += operation
        }

        override suspend fun upsert(entity: FavoriteBookEntity) {
            beforeWrite("save:${entity.id}")
            rows.value = rows.value + (entity.id to entity)
        }

        override suspend fun deleteById(id: String) {
            beforeWrite("delete:$id")
            rows.value = rows.value - id
        }
    }

    @Test
    fun `다른 화면의 key도 같은 actor에서 저장 해제 저장 순서로 처리한다`() = runTest {
        val dao = DelayedDao()
        val factory = DefaultFavoriteMutationKeyFactory(dao)
        val client = SwrCachePlus(backgroundScope)
        client.getMutation(factory.create(), Marker.None).use { search ->
            client.getMutation(factory.create(), Marker.None).use { detail ->
                launch { search.mutate(FavoriteCommand(book("a"), true)) }
                runCurrent()
                launch { detail.mutate(FavoriteCommand(book("a"), false)) }
                runCurrent()
                launch { search.mutate(FavoriteCommand(book("a"), true)) }
                runCurrent()
                assertEquals(listOf("save:a"), dao.started)
                dao.gate.complete(Unit)
                runCurrent()
                assertEquals(listOf("save:a", "delete:a", "save:a"), dao.writes)
                assertEquals(book("a"), dao.rows.value.getValue("a").toBook())
            }
        }
    }

    @Test
    fun `Mutation 대기자가 취소되어도 참조가 살아 있으면 시작한 DAO는 완료한다`() = runTest {
        val dao = DelayedDao()
        val client = SwrCachePlus(backgroundScope)
        client.getMutation(DefaultFavoriteMutationKeyFactory(dao).create(), Marker.None).use { mutation ->
            val caller = launch { mutation.mutate(FavoriteCommand(book("a"), true)) }
            runCurrent()
            assertEquals(listOf("save:a"), dao.started)
            caller.cancel()
            runCurrent()
            dao.gate.complete(Unit)
            runCurrent()
            assertTrue(caller.isCancelled)
            assertEquals(setOf("a"), dao.rows.value.keys)
        }
    }

    @Test
    fun `DAO 실패는 호출자에게 전달하고 같은 actor의 다음 쓰기를 막지 않는다`() = runTest {
        val dao = DelayedDao().apply { gate.complete(Unit) }
        val error = IOException("disk full")
        dao.failure = error
        val client = SwrCachePlus(backgroundScope)
        client.getMutation(DefaultFavoriteMutationKeyFactory(dao).create(), Marker.None).use { mutation ->
            val result = runCatching { mutation.mutate(FavoriteCommand(book("a"), true)) }
            assertTrue(result.exceptionOrNull() is IOException)
            assertEquals(error.message, result.exceptionOrNull()?.message)
            assertTrue(dao.rows.value.isEmpty())
            dao.failure = null
            mutation.mutate(FavoriteCommand(book("b"), true))
            assertEquals(listOf("save:b"), dao.writes)
            assertEquals(setOf("b"), dao.rows.value.keys)
        }
    }

    @Test
    fun `DAO 취소 예외를 Mutation key가 감싸지 않고 그대로 전달한다`() = runTest {
        val cancellation = CancellationException("cancelled")
        val dao = DelayedDao().apply { gate.complete(Unit); failure = cancellation }
        val key = DefaultFavoriteMutationKeyFactory(dao).create()
        val result = runCatching { key.mutate(MutationReceiver, FavoriteCommand(book("a"), true)) }
        assertSame(cancellation, result.exceptionOrNull())
        assertTrue(dao.cancelled)
        assertEquals(emptyList<String>(), dao.writes)
    }

    @Test
    fun `actor 자체 취소는 진행 중 DAO도 취소하며 독립 앱 scope 보장은 없다`() = runTest {
        val dao = DelayedDao()
        val actorScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val client = SwrCachePlus(actorScope)
        val mutation = client.getMutation(DefaultFavoriteMutationKeyFactory(dao).create(), Marker.None)
        val caller = launch { mutation.mutate(FavoriteCommand(book("a"), true)) }
        try {
            runCurrent()
            assertEquals(listOf("save:a"), dao.started)
            actorScope.cancel()
            runCurrent()
            dao.gate.complete(Unit)
            runCurrent()
            assertTrue(dao.cancelled)
            assertEquals(emptyList<String>(), dao.writes)
            assertTrue(dao.rows.value.isEmpty())
        } finally {
            // Soil alpha15의 actor 취소는 대기 Deferred를 완료하지 않으므로 호출자도 명시적으로 취소합니다.
            caller.cancel()
            mutation.close()
            actorScope.cancel()
        }
    }

    private fun book(id: String) = Book(
        id = id, isbn10 = null, isbn13 = null, title = id, authors = emptyList(), translators = emptyList(),
        publisher = "", publishedDate = null, price = null, salePrice = null, thumbnailUrl = "",
        contents = "", url = "", status = "",
    )
}
