package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.easyhooon.booksearch.core.data.DefaultSearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.model.SearchPage
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.network.KakaoBookService
import io.github.easyhooon.booksearch.core.network.model.DocumentResponse
import io.github.easyhooon.booksearch.core.network.model.MetaResponse
import io.github.easyhooon.booksearch.core.network.model.SearchBookResponse
import io.github.easyhooon.booksearch.core.testing.FakeFavoriteWriter
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
import soil.query.InfiniteQueryKey
import soil.query.QueryOptionsOverride
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.compose.LocalMutationClient
import soil.query.compose.LocalQueryClient
import soil.query.copy
import java.io.IOException
import kotlin.time.Duration

/** 실제 검색 key와 Soil을 연결해 재조회와 추가 로딩의 재시도 대상을 확인합니다. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSoilQueryApi::class)
class SearchRetryTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = SwrCachePlus(clientScope)
    private val writer = FakeFavoriteWriter()
    private val requestedPages = mutableListOf<Int>()
    private var failedPage: Int? = null
    private var shown by mutableStateOf(true)
    private lateinit var latest: SearchUiState

    private val service = object : KakaoBookService {
        override suspend fun searchBooks(query: String, sort: String, page: Int, size: Int): SearchBookResponse {
            requestedPages += page
            if (page == failedPage) throw IOException("offline")
            return SearchBookResponse(
                meta = MetaResponse(totalCount = 60, pageableCount = 60, isEnd = page == 3),
                documents = (1..SearchPaging.PAGE_SIZE).map {
                    DocumentResponse(title = "p$page-$it", url = "https://example.invalid/$page/$it")
                },
            )
        }
    }
    private val productionFactory = DefaultSearchBooksQueryKeyFactory(service)
    private val factory = SearchBooksQueryKeyFactory { query, sort ->
        val key = productionFactory.create(query, sort)
        object : InfiniteQueryKey<SearchPage, Int> by key {
            // 만료 대기를 줄이고 자동 재시도를 꺼서 사용자 Retry만 확인합니다.
            override fun onConfigureOptions(): QueryOptionsOverride = { defaults ->
                key.onConfigureOptions()!!.invoke(defaults).copy(staleTime = Duration.ZERO, shouldRetry = { false })
            }
        }
    }

    @After
    fun tearDown() {
        clientScope.cancel()
    }

    private fun search() {
        composeRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            val registry = retainRetainedValuesStoreRegistry()
            CompositionLocalProvider(LocalQueryClient provides client, LocalMutationClient provides client) {
                if (shown) {
                    saveableStateHolder.SaveableStateProvider("search") {
                        registry.LocalRetainedValuesStoreProvider("search") {
                            latest = searchPresenter(factory, emptySet(), writer.key)
                        }
                    }
                }
            }
        }
        send(SearchEvent.InputChanged("코틀린"))
        send(SearchEvent.Submit)
        awaitResults(SearchPaging.PAGE_SIZE)
    }

    private fun send(event: SearchEvent) {
        composeRule.runOnIdle { latest.eventSink(event) }
        composeRule.waitForIdle()
    }

    private fun awaitResults(count: Int) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            val content = latest.content as? SearchContent.Results
            content != null && !content.isLoadingMore && content.loadMoreError == null && content.books.size == count
        }
    }

    private fun awaitFailure() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            (latest.content as? SearchContent.Results)?.loadMoreError != null
        }
        assertEquals(ErrorMessage.Network, (latest.content as SearchContent.Results).loadMoreError)
    }

    private fun reenterWithFailure(page: Int) {
        composeRule.runOnIdle { shown = false }
        composeRule.waitForIdle()
        failedPage = page
        // Soil의 epoch는 실제 시계의 초 단위이므로 성공 시각보다 다음 초에 재구독합니다.
        Thread.sleep(1_100)
        composeRule.runOnIdle { shown = true }
        composeRule.waitForIdle()
        awaitFailure()
    }

    @Test
    fun `만료된 첫 페이지 재조회가 실패하면 다음 페이지 대신 첫 페이지를 재시도한다`() {
        search()
        reenterWithFailure(page = 1)
        assertEquals(listOf(1, 1), requestedPages)

        failedPage = null
        send(SearchEvent.Retry)
        composeRule.waitUntil(timeoutMillis = 5_000) { requestedPages.size == 3 }
        assertEquals(listOf(1, 1, 1), requestedPages)
        awaitResults(SearchPaging.PAGE_SIZE)
    }

    @Test
    fun `여러 페이지 재조회 중 실패해도 받지 않은 다음 페이지로 건너뛰지 않는다`() {
        search()
        send(SearchEvent.LoadMore)
        awaitResults(SearchPaging.PAGE_SIZE * 2)
        reenterWithFailure(page = 2)
        assertEquals(listOf(1, 2, 1, 2), requestedPages)

        failedPage = null
        send(SearchEvent.Retry)
        composeRule.waitUntil(timeoutMillis = 5_000) { requestedPages.size >= 5 }
        assertEquals(1, requestedPages[4])
        awaitResults(SearchPaging.PAGE_SIZE * 2)
        assertEquals(listOf(1, 2, 1, 2, 1, 2), requestedPages)
    }

    @Test
    fun `추가 로딩 실패는 첫 페이지 재조회 없이 실패한 다음 페이지를 재시도한다`() {
        search()
        failedPage = 2
        send(SearchEvent.LoadMore)
        awaitFailure()
        assertEquals(listOf(1, 2), requestedPages)

        failedPage = null
        send(SearchEvent.Retry)
        awaitResults(SearchPaging.PAGE_SIZE * 2)
        assertEquals(listOf(1, 2, 2), requestedPages)
    }
}
