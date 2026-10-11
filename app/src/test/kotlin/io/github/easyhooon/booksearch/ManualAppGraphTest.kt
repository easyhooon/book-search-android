package io.github.easyhooon.booksearch

import android.app.Application
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.network.NetworkConfig
import io.github.easyhooon.booksearch.core.testing.testBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import soil.query.MutationReceiver
import soil.query.SubscriptionReceiver

/** 수동 조립에서 바뀌기 쉬운 앱·화면 객체 수명과 세 화면의 같은 저장 경로를 확인합니다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ManualAppGraphTest {

    private fun appGraph(): AppGraph = ManualAppGraph.Factory.create(
        context = RuntimeEnvironment.getApplication(),
        networkConfig = NetworkConfig(apiKey = "", enableHttpLog = false),
    )

    @Test
    fun `화면 그래프는 자기 context를 유지하고 새 화면 그래프와 공유하지 않는다`() {
        val app = appGraph()
        val search = app.createSearchScreenGraph()
        val favorites = app.createFavoritesScreenGraph()
        val detail = app.createBookDetailScreenGraph()
        val searchContext = search.searchScreenContext
        val favoritesContext = favorites.favoritesScreenContext
        val detailContext = detail.bookDetailScreenContext

        assertSame(searchContext, search.searchScreenContext)
        assertSame(favoritesContext, favorites.favoritesScreenContext)
        assertSame(detailContext, detail.bookDetailScreenContext)
        assertNotSame(searchContext, app.createSearchScreenGraph().searchScreenContext)
        assertNotSame(favoritesContext, app.createFavoritesScreenGraph().favoritesScreenContext)
        assertNotSame(detailContext, app.createBookDetailScreenGraph().bookDetailScreenContext)
        assertNotSame(
            searchContext.searchBooksQueryKeyFactory,
            app.createSearchScreenGraph().searchScreenContext.searchBooksQueryKeyFactory,
        )
    }

    @Test
    fun `쿼리 클라이언트와 이미지 로더는 앱 그래프 안에서만 공유한다`() {
        val first = appGraph()
        val second = appGraph()
        val firstImages = first.imageLoader
        val secondImages = second.imageLoader
        try {
            assertSame(first.swrClient, first.swrClient)
            assertSame(firstImages, first.imageLoader)
            assertNotSame(first.swrClient, second.swrClient)
            assertNotSame(firstImages, secondImages)
        } finally {
            firstImages.shutdown()
            secondImages.shutdown()
        }
    }

    @Test
    fun `세 화면의 저장 key는 서로 다른 객체지만 같은 Mutation ID를 쓴다`() {
        val app = appGraph()
        val search = app.createSearchScreenGraph().searchScreenContext.favoriteMutationKey
        val favorites = app.createFavoritesScreenGraph().favoritesScreenContext.favoriteMutationKey
        val detail = app.createBookDetailScreenGraph().bookDetailScreenContext.favoriteMutationKey

        assertEquals(search.id, favorites.id)
        assertEquals(search.id, detail.id)
        assertNotSame(search, favorites)
        assertNotSame(search, detail)
        assertNotSame(favorites, detail)
    }

    @Test
    fun `세 화면에서 저장 수정 삭제한 도서를 모든 화면이 같은 Room 데이터로 읽는다`() =
        runBlocking(Dispatchers.Default) {
            // 실제 Room 작업은 IO 스레드를 사용하므로 가상 시간 대신 제한된 실제 시간으로 기다립니다.
            withTimeout(30_000) {
                val app = appGraph()
                val search = app.createSearchScreenGraph().searchScreenContext
                val favorites = app.createFavoritesScreenGraph().favoritesScreenContext
                val detail = app.createBookDetailScreenGraph().bookDetailScreenContext
                val book = testBook("manual-graph-book", title = "저장 전 제목")
                val detailKey = detail.favoriteBookSubscriptionKeyFactory.create(book.id)

                // key를 직접 호출해 DI의 쓰기·읽기 연결만 확인합니다. Soil actor 순서·수명은 기존 테스트의 범위입니다.
                suspend fun assertSaved(expected: Book?) {
                    assertEquals(
                        listOfNotNull(expected?.id).toSet(),
                        search.favoriteIdsSubscriptionKey.subscribe(SubscriptionReceiver).first(),
                    )
                    assertEquals(
                        listOfNotNull(expected),
                        favorites.favoriteBooksSubscriptionKey.subscribe(SubscriptionReceiver).first(),
                    )
                    assertEquals(expected, detailKey.subscribe(SubscriptionReceiver).first())
                }

                assertSaved(null)
                search.favoriteMutationKey.mutate(MutationReceiver, FavoriteCommand(book, favorite = true))
                assertSaved(book)

                val updated = book.copy(title = "상세에서 바꾼 제목")
                detail.favoriteMutationKey.mutate(MutationReceiver, FavoriteCommand(updated, favorite = true))
                assertSaved(updated)

                favorites.favoriteMutationKey.mutate(MutationReceiver, FavoriteCommand(updated, favorite = false))
                assertSaved(null)
            }
        }
}
