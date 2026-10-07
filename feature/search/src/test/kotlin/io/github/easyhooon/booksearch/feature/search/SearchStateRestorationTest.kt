package io.github.easyhooon.booksearch.feature.search

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryId
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.model.SearchPage
import io.github.easyhooon.booksearch.core.model.SearchPaging
import io.github.easyhooon.booksearch.core.model.SearchSort
import io.github.easyhooon.booksearch.core.testing.FakeFavoriteWriter
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
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.buildInfiniteQueryKey
import soil.query.compose.LocalMutationClient
import soil.query.compose.LocalQueryClient

/** 프로세스가 재생성되어도 검색 Presenter의 저장 데이터(입력·확정 검색어·정렬)를 복원하는지 확인합니다. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSoilQueryApi::class)
class SearchStateRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = SwrCachePlus(clientScope)
    private val writer = FakeFavoriteWriter()
    private lateinit var latest: SearchUiState

    private val factory = SearchBooksQueryKeyFactory { query, sort ->
        buildInfiniteQueryKey(
            id = SearchBooksQueryId(query, sort),
            fetch = { page -> SearchPage(page = page, books = emptyList(), isEnd = true, totalCount = 0) },
            initialParam = { SearchPaging.FIRST_PAGE },
            loadMoreParam = { null },
        )
    }

    @After
    fun tearDown() {
        clientScope.cancel()
    }

    private fun send(event: SearchEvent) {
        composeRule.runOnIdle { latest.eventSink(event) }
        composeRule.waitForIdle()
    }

    @Test
    fun `프로세스 재생성 뒤에도 입력과 확정 검색어와 정렬을 복원한다`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CompositionLocalProvider(LocalQueryClient provides client, LocalMutationClient provides client) {
                latest = searchPresenter(
                    queryKeyFactory = factory,
                    favoriteIds = emptySet(),
                    favoriteMutationKey = writer.key,
                )
            }
        }
        send(SearchEvent.InputChanged("코틀린"))
        send(SearchEvent.Submit)
        send(SearchEvent.InputChanged("코틀린 인 액션"))
        send(SearchEvent.SortSelected(SearchSort.Latest))

        restorationTester.emulateSavedInstanceStateRestore()

        assertEquals("코틀린 인 액션", latest.input)
        assertEquals("코틀린", latest.submittedQuery)
        assertEquals(SearchSort.Latest, latest.sort)
    }
}
