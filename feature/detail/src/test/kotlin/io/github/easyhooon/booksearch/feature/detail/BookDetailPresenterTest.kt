package io.github.easyhooon.booksearch.feature.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.testing.FakeFavoriteWriter
import io.github.easyhooon.booksearch.core.testing.MainDispatcherRule
import io.github.easyhooon.booksearch.core.testing.awaitItemMatching
import io.github.easyhooon.booksearch.core.testing.presenterFlow
import io.github.easyhooon.booksearch.core.testing.testBook
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import java.io.IOException

/** Android용 Compose runtime이 Trace·Log를 호출하므로 Robolectric에서 실행합니다. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSoilQueryApi::class)
class BookDetailPresenterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val writer = FakeFavoriteWriter()
    private val snapshot = testBook("k", title = "코틀린 인 액션", price = 36_000)

    /** Root가 Room Subscription에서 읽어 넘기는 값입니다. */
    private var roomBook by mutableStateOf<Book?>(null)
    private var isRoomLoaded by mutableStateOf(false)
    private lateinit var latest: BookDetailUiState

    private fun runPresenterTest(validate: suspend ReceiveTurbine<BookDetailUiState>.() -> Unit) = runTest {
        val client = SwrCachePlus(backgroundScope)
        presenterFlow(client) {
            bookDetailPresenter(
                snapshot = snapshot,
                roomBook = roomBook,
                isRoomLoaded = isRoomLoaded,
                favoriteMutationKey = writer.key,
            )
        }.onEach { latest = it }.test {
            validate()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun send(event: BookDetailEvent) = latest.eventSink(event)

    private fun loadRoom(book: Book?) {
        roomBook = book
        isRoomLoaded = true
    }

    @Test
    fun `Room 값을 받기 전에는 스냅샷을 보여 주고 하트를 잠근다`() = runPresenterTest {
        val initial = awaitItem()
        assertEquals(snapshot, initial.book)
        assertFalse(initial.isFavorite)
        assertFalse(initial.isFavoriteEnabled)

        send(BookDetailEvent.ToggleFavorite)
        loadRoom(null)
        awaitItemMatching { it.isFavoriteEnabled }
        assertEquals(emptyList<FavoriteCommand>(), writer.commands)
    }

    @Test
    fun `Room에 저장된 값이 있으면 Room 값과 즐겨찾기 상태를 보여 준다`() = runPresenterTest {
        val saved = snapshot.copy(salePrice = 30_000)
        loadRoom(saved)

        val state = awaitItemMatching { it.isFavoriteEnabled }
        assertEquals(saved, state.book)
        assertTrue(state.isFavorite)
    }

    @Test
    fun `하트를 누르면 Room 상태의 반대를 목표 상태로 요청한다`() = runPresenterTest {
        loadRoom(null)
        awaitItemMatching { it.isFavoriteEnabled }

        send(BookDetailEvent.ToggleFavorite)
        awaitItemMatching { !it.isFavoriteEnabled }
        awaitItemMatching { it.isFavoriteEnabled }
        // Room 쓰기는 FakeFavoriteWriter가 대신하므로 반영은 테스트가 직접 흉내 냅니다.
        loadRoom(snapshot)
        awaitItemMatching { it.isFavorite }

        send(BookDetailEvent.ToggleFavorite)
        awaitItemMatching { !it.isFavoriteEnabled }
        loadRoom(null)
        awaitItemMatching { !it.isFavorite && it.isFavoriteEnabled }

        assertEquals(
            listOf(FavoriteCommand(snapshot, favorite = true), FavoriteCommand(snapshot, favorite = false)),
            writer.commands,
        )
    }

    @Test
    fun `해제해서 Room 행이 사라져도 스냅샷으로 상세를 유지한다`() = runPresenterTest {
        val saved = snapshot.copy(contents = "저장된 소개")
        loadRoom(saved)
        awaitItemMatching { it.book == saved }

        loadRoom(null)
        val state = awaitItemMatching { !it.isFavorite }
        assertEquals(snapshot, state.book)
    }

    @Test
    fun `저장 중에는 하트를 잠그고 중복 요청을 보내지 않는다`() = runPresenterTest {
        val gate = CompletableDeferred<Unit>()
        writer.write = { gate.await() }
        loadRoom(null)
        awaitItemMatching { it.isFavoriteEnabled }

        send(BookDetailEvent.ToggleFavorite)
        awaitItemMatching { !it.isFavoriteEnabled }
        send(BookDetailEvent.ToggleFavorite)

        gate.complete(Unit)
        awaitItemMatching { it.isFavoriteEnabled }
        assertEquals(1, writer.commands.size)
    }

    @Test
    fun `저장에 실패하면 메시지를 보여 주고 확인 후 내린다`() = runPresenterTest {
        writer.write = { throw IOException("disk") }
        loadRoom(null)
        awaitItemMatching { it.isFavoriteEnabled }

        send(BookDetailEvent.ToggleFavorite)
        val failed = awaitItemMatching { it.userMessage != null }
        assertEquals(ErrorMessage.FavoriteWrite, failed.userMessage)
        assertFalse(failed.isFavorite)

        send(BookDetailEvent.UserMessageShown)
        awaitItemMatching { it.userMessage == null }
    }
}
