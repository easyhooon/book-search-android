package io.github.easyhooon.booksearch.feature.search

import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryId
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.data.SearchPageFetchException
import io.github.easyhooon.booksearch.core.model.SearchPage
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.testing.FakeFavoriteMutation
import io.github.easyhooon.booksearch.core.testing.testBook
import io.github.easyhooon.booksearch.core.ui.ErrorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import soil.query.InfiniteQueryKey
import soil.query.QueryOptionsOverride
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.buildInfiniteQueryKey
import soil.query.compose.LocalMutationClient
import soil.query.compose.LocalQueryClient
import soil.query.copy
import java.io.IOException
import java.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 검색 루트가 다른 탭에 가려져 컴포지션에서 빠져도 백스택에 남아 있는 동안은 검색 결과를 잃지 않는지 확인합니다.
 *
 * Soil은 구독이 끊기고 keepAliveTime(기본 5초)이 지나면 쿼리를 내리면서 성공 상태만 캐시에 남깁니다.
 * 추가 로딩이 실패한 상태로 탭을 오래 비우면 목록이 지워지므로, NavHost처럼 엔트리별 retain 저장소 안에서
 * (saveable·retain 저장소를 함께 둠) 화면을 빼고 다시 넣어 봅니다.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSoilQueryApi::class)
class SearchQueryRetentionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = SwrCachePlus(clientScope)
    private val writer = FakeFavoriteMutation()
    private val requestedPages = mutableListOf<Int>()
    private var failPage2 = true

    private val factory = SearchBooksQueryKeyFactory { query, sort ->
        val base = buildInfiniteQueryKey(
            id = SearchBooksQueryId(query, sort),
            fetch = { page ->
                requestedPages += page
                if (page == 2 && failPage2) throw SearchPageFetchException(page, IOException("offline"))
                page(page, isEnd = page == 2)
            },
            initialParam = { SearchPaging.FIRST_PAGE },
            loadMoreParam = { chunks -> chunks.lastOrNull()?.data?.let(SearchPaging::nextPage) },
        )
        // 앱의 key와 같이 5분 동안은 성공 결과를 새로 받지 않고, 오류 화면 전이만 보도록 자동 재시도는 끕니다.
        object : InfiniteQueryKey<SearchPage, Int> by base {
            override fun onConfigureOptions(): QueryOptionsOverride = { it.copy(staleTime = 5.minutes, shouldRetry = { false }) }
        }
    }

    private var shown by mutableStateOf(true)
    private lateinit var latest: SearchUiState

    @After
    fun tearDown() {
        clientScope.cancel()
    }

    private fun setContent() {
        composeRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            val registry = retainRetainedValuesStoreRegistry()
            CompositionLocalProvider(
                LocalQueryClient provides client,
                LocalMutationClient provides client,
            ) {
                if (shown) {
                    saveableStateHolder.SaveableStateProvider("search") {
                        registry.LocalRetainedValuesStoreProvider("search") {
                            latest = searchPresenter(
                                queryKeyFactory = factory,
                                favoriteIds = emptySet(),
                                favoriteMutationKey = writer.key,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun send(event: SearchEvent) {
        composeRule.runOnIdle { latest.eventSink(event) }
        composeRule.waitForIdle()
    }

    private fun awaitContent(predicate: (SearchContent) -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) { predicate(latest.content) }
    }

    /** keepAliveTime이 지나도록 Soil의 main dispatcher 지연 작업을 진행합니다. */
    private fun passKeepAliveTime() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        composeRule.waitForIdle()
    }

    @Test
    fun `추가 로딩 실패 뒤 다른 탭에 오래 있다가 돌아와도 목록과 재시도가 남는다`() {
        setContent()
        send(SearchEvent.InputChanged("코틀린"))
        send(SearchEvent.Submit)
        awaitContent { it is SearchContent.Results }
        send(SearchEvent.LoadMore)
        awaitContent { (it as? SearchContent.Results)?.loadMoreError != null }

        shown = false
        composeRule.waitForIdle()
        passKeepAliveTime()
        shown = true
        composeRule.waitForIdle()

        val content = latest.content as SearchContent.Results
        assertEquals(SearchPaging.PAGE_SIZE, content.books.size)
        assertEquals(ErrorMessage.Network, content.loadMoreError)
        assertEquals(listOf(1, 2), requestedPages)

        failPage2 = false
        send(SearchEvent.Retry)
        awaitContent { (it as? SearchContent.Results)?.books?.size == SearchPaging.PAGE_SIZE * 2 }
        assertEquals(listOf(1, 2, 2), requestedPages)
    }

    @Test
    fun `여러 페이지를 받은 뒤 다른 탭에 오래 있다가 돌아와도 1페이지부터 다시 받지 않는다`() {
        failPage2 = false
        setContent()
        send(SearchEvent.InputChanged("코틀린"))
        send(SearchEvent.Submit)
        awaitContent { it is SearchContent.Results }
        send(SearchEvent.LoadMore)
        awaitContent { (it as? SearchContent.Results)?.books?.size == SearchPaging.PAGE_SIZE * 2 }

        shown = false
        composeRule.waitForIdle()
        passKeepAliveTime()
        shown = true
        composeRule.waitForIdle()

        assertEquals(SearchPaging.PAGE_SIZE * 2, (latest.content as SearchContent.Results).books.size)
        assertEquals(listOf(1, 2), requestedPages)
    }

    private companion object {
        fun page(page: Int, isEnd: Boolean) = SearchPage(
            page = page,
            books = (1..SearchPaging.PAGE_SIZE).map { testBook("p$page-$it") },
            isEnd = isEnd,
            totalCount = 1_000,
        )
    }
}
