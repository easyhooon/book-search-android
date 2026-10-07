package io.github.easyhooon.booksearch.core.ui

import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteTogglerTest {

    private val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

    /** 테스트마다 쓰기 동작을 바꿉니다. 기본은 바로 성공합니다. */
    private var write: suspend (FavoriteCommand) -> Unit = {}
    private val toggler = FavoriteToggler(scope) { command -> write(command) }

    @Test
    fun `같은 도서는 쓰기가 끝날 때까지 한 번만 요청한다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val commands = mutableListOf<FavoriteCommand>()
        write = { command ->
            commands += command
            gate.await()
        }

        toggler.setFavorite(book("a"), favorite = true)
        toggler.setFavorite(book("a"), favorite = false)
        toggler.setFavorite(book("b"), favorite = true)

        assertEquals(setOf("a", "b"), toggler.pendingIds)
        assertEquals(listOf(FavoriteCommand(book("a"), true), FavoriteCommand(book("b"), true)), commands)

        gate.complete(Unit)

        assertEquals(emptySet<String>(), toggler.pendingIds)
        assertFalse(toggler.hasFailure)
    }

    @Test
    fun `쓰기가 실패하면 실패를 알리고 진행 상태를 푼다`() = runTest {
        write = { throw IOException("disk") }
        toggler.setFavorite(book("a"), favorite = true)

        assertTrue(toggler.hasFailure)
        assertEquals(emptySet<String>(), toggler.pendingIds)

        toggler.consumeFailure()
        assertFalse(toggler.hasFailure)
    }

    @Test
    fun `소유자가 scope를 취소하면 대기를 멈추고 실패로 알리지 않는다`() = runTest {
        write = { CompletableDeferred<Unit>().await() }
        toggler.setFavorite(book("a"), favorite = true)

        scope.cancel()

        assertEquals(emptySet<String>(), toggler.pendingIds)
        assertFalse(toggler.hasFailure)
    }

    private fun book(id: String) = Book(
        id = id,
        isbn10 = null,
        isbn13 = null,
        title = "제목 $id",
        authors = emptyList(),
        translators = emptyList(),
        publisher = "",
        publishedDate = null,
        price = null,
        salePrice = null,
        thumbnailUrl = "",
        contents = "",
        url = "",
        status = "",
    )
}
