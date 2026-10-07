package io.github.easyhooon.booksearch.feature.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import io.github.easyhooon.booksearch.core.model.FavoriteSort
import io.github.easyhooon.booksearch.core.model.PriceFilter
import io.github.easyhooon.booksearch.core.testing.testBook
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 조회 조건이 바뀌면 새 목록이므로 맨 위에서 시작하는지 확인합니다. */
@RunWith(RobolectricTestRunner::class)
class FavoritesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val books = (10..39).map { testBook("b$it", price = it * 1_000) }

    private var uiState by mutableStateOf(
        FavoritesUiState(
            input = "",
            submittedQuery = "",
            sort = FavoriteSort.TitleAscending,
            priceFilter = PriceFilter(),
            content = FavoritesContent.Results(books),
            pendingFavoriteIds = emptySet(),
            userMessage = null,
            eventSink = {},
        ),
    )

    private fun scrollToEnd() {
        composeRule.setContent { FavoritesScreen(uiState = uiState, onBookClick = {}) }
        composeRule.onNodeWithTag(FavoritesTestTags.RESULT_GRID).performScrollToIndex(books.lastIndex)
        composeRule.onNodeWithText("제목 b10").assertDoesNotExist()
    }

    @Test
    fun `정렬을 바꾸면 목록 맨 위로 돌아간다`() {
        scrollToEnd()

        uiState = uiState.copy(
            sort = FavoriteSort.TitleDescending,
            content = FavoritesContent.Results(books.reversed()),
        )

        composeRule.onNodeWithText("제목 b39").assertIsDisplayed()
    }

    @Test
    fun `가격 필터를 바꾸면 목록 맨 위로 돌아간다`() {
        scrollToEnd()

        uiState = uiState.copy(
            priceFilter = PriceFilter(min = 12_000),
            content = FavoritesContent.Results(books.drop(2)),
        )

        composeRule.onNodeWithText("제목 b12").assertIsDisplayed()
    }

    @Test
    fun `검색어를 확정하면 목록 맨 위로 돌아간다`() {
        scrollToEnd()

        uiState = uiState.copy(
            submittedQuery = "제목 b",
            content = FavoritesContent.Results(books.drop(1)),
        )

        composeRule.onNodeWithText("제목 b11").assertIsDisplayed()
    }

    @Test
    fun `조건이 같으면 하트를 해제해도 스크롤 위치를 유지한다`() {
        scrollToEnd()

        uiState = uiState.copy(content = FavoritesContent.Results(books.dropLast(1)))

        composeRule.onNodeWithText("제목 b38").assertIsDisplayed()
        composeRule.onNodeWithText("제목 b10").assertDoesNotExist()
    }
}
