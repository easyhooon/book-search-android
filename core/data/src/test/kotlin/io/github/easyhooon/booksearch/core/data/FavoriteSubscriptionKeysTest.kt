package io.github.easyhooon.booksearch.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.easyhooon.booksearch.core.database.BookSearchDatabase
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import soil.query.SubscriptionKey
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.core.Marker
import soil.query.core.getOrThrow
import soil.query.core.isNone
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

/**
 * Room → Subscription key → Soil 클라이언트 연결을 확인합니다.
 *
 * 화면과 같은 경로를 쓰도록 쓰기는 실제 Mutation key로 하고, 구독은 rememberSubscription처럼
 * SubscriptionRef의 resume과 join으로 시작합니다. Presenter 테스트는 이 결과를 상태 값으로 받습니다.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalSoilQueryApi::class)
class FavoriteSubscriptionKeysTest {

    private lateinit var database: BookSearchDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BookSearchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val dao get() = database.favoriteBookDao()

    // Room Flow는 실제 스레드에서 방출되므로 Turbine의 실제 시간 대기(기본 3초)가 Robolectric 첫 초기화에 걸리지 않게 늘립니다.
    private val ROOM_TIMEOUT = 30.seconds

    private fun book(id: String, title: String = "제목 $id") = Book(
        id = id,
        isbn10 = null,
        isbn13 = id,
        title = title,
        authors = listOf("저자1", "저자, 2"),
        translators = listOf("역자"),
        publisher = "출판사",
        publishedDate = LocalDate.of(2024, 1, 31),
        price = 36_000,
        salePrice = 32_400,
        thumbnailUrl = "https://example.com/$id.jpg",
        contents = "소개 $id",
        url = "https://example.com/$id",
        status = "정상판매",
    )

    private fun TestScope.mutation() = SwrCachePlus(backgroundScope)
        .getMutation(DefaultFavoriteMutationKeyFactory(dao).create(), Marker.None)

    /** rememberSubscription과 같이 resume·join으로 구독을 시작하고, 값을 받은 상태만 내보냅니다. */
    private fun <T> TestScope.subscribe(key: SubscriptionKey<T>): Flow<T> {
        val ref = SwrCachePlus(backgroundScope).getSubscription(key, Marker.None)
        backgroundScope.launch { ref.resume() }
        backgroundScope.launch { ref.join() }
        return ref.state.filterNot { it.reply.isNone }.map { it.reply.getOrThrow() }
    }

    private suspend fun <T> ReceiveTurbine<T>.awaitValue(expected: T) {
        while (true) {
            if (awaitItem() == expected) return
        }
    }

    @Test
    fun `ID 구독은 빈 집합에서 시작해 추가와 해제를 반영한다`() = runTest {
        val mutation = mutation()
        subscribe(DefaultFavoriteIdsSubscriptionKey(dao)).test(timeout = ROOM_TIMEOUT) {
            assertEquals(emptySet<String>(), awaitItem())

            mutation.mutate(FavoriteCommand(book("a"), favorite = true))
            awaitValue(setOf("a"))
            mutation.mutate(FavoriteCommand(book("b"), favorite = true))
            awaitValue(setOf("a", "b"))
            mutation.mutate(FavoriteCommand(book("a"), favorite = false))
            awaitValue(setOf("b"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `목록 구독은 저장한 도서를 같은 값으로 돌려준다`() = runTest {
        val mutation = mutation()
        val saved = book("a", title = "코틀린 인 액션")
        subscribe(DefaultFavoriteBooksSubscriptionKey(dao)).test(timeout = ROOM_TIMEOUT) {
            assertEquals(emptyList<Book>(), awaitItem())

            mutation.mutate(FavoriteCommand(saved, favorite = true))
            awaitValue(listOf(saved))
            mutation.mutate(FavoriteCommand(saved, favorite = false))
            awaitValue(emptyList())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `상세 구독은 해당 도서만 따라가고 해제하면 null이 된다`() = runTest {
        val mutation = mutation()
        val target = book("a")
        subscribe(DefaultFavoriteBookSubscriptionKeyFactory(dao).create("a")).test(timeout = ROOM_TIMEOUT) {
            assertNull(awaitItem())

            mutation.mutate(FavoriteCommand(book("b"), favorite = true))
            mutation.mutate(FavoriteCommand(target, favorite = true))
            awaitValue(target)
            mutation.mutate(FavoriteCommand(target, favorite = false))
            awaitValue(null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `같은 key를 두 화면이 구독하면 하나의 상태를 함께 본다`() = runTest {
        val mutation = mutation()
        val client = SwrCachePlus(backgroundScope)
        val key = DefaultFavoriteIdsSubscriptionKey(dao)
        val first = client.getSubscription(key, Marker.None)
        val second = client.getSubscription(key, Marker.None)
        listOf(first, second).forEach { ref ->
            backgroundScope.launch { ref.resume() }
            backgroundScope.launch { ref.join() }
        }

        second.state.filterNot { it.reply.isNone }.map { it.reply.getOrThrow() }.test(timeout = ROOM_TIMEOUT) {
            assertEquals(emptySet<String>(), awaitItem())
            mutation.mutate(FavoriteCommand(book("a"), favorite = true))
            awaitValue(setOf("a"))
            assertEquals(setOf("a"), first.state.value.reply.getOrThrow())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `구독을 놓은 사이의 쓰기는 다시 구독할 때 반영된다`() = runBlocking {
        // Soil은 구독자가 없어도 keepAliveTime 동안 Flow를 수집하면서 그 값을 버립니다. 이 시간은 실제 시간으로 흐르므로
        // 가상 시간이 바로 지나가는 runTest 대신 실제 시간 scope에서, keepAliveTime 기본값(5초)보다 짧게 기다립니다.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val client = SwrCachePlus(scope)
            val mutation = client.getMutation(DefaultFavoriteMutationKeyFactory(dao).create(), Marker.None)
            val key = DefaultFavoriteIdsSubscriptionKey(dao)

            // 검색 화면이 구독하다가 상세로 이동하며 구독을 놓습니다.
            val leaving = client.getSubscription(key, Marker.None)
            val collecting = scope.launch { leaving.resume() }
            withTimeout(ROOM_TIMEOUT) { leaving.state.first { !it.reply.isNone } }
            collecting.cancel()
            leaving.close()

            // 상세에서 저장한 뒤 곧바로 돌아와 다시 구독합니다.
            mutation.mutate(FavoriteCommand(book("a"), favorite = true))
            val returning = client.getSubscription(key, Marker.None)
            scope.launch { returning.resume() }
            withTimeout(2.seconds) {
                returning.state.first { !it.reply.isNone && it.reply.getOrThrow() == setOf("a") }
            }
            returning.close()
        } finally {
            scope.cancel()
        }
    }
}
