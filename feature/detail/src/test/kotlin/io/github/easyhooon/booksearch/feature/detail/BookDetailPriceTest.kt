package io.github.easyhooon.booksearch.feature.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.easyhooon.booksearch.core.testing.testBook
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDetailPriceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `상세에서 양수 정가만 금액으로 표시하고 나머지는 가격 정보 없음으로 표시한다`() {
        var price by mutableStateOf<Int?>(1)
        composeRule.setContent {
            BookDetailScreen(
                uiState = BookDetailUiState(
                    book = testBook("a", price = price),
                    isFavorite = false,
                    isFavoriteEnabled = true,
                    userMessage = null,
                    eventSink = {},
                ),
                onBack = {},
            )
        }
        composeRule.onNodeWithText("1원").performScrollTo().assertIsDisplayed()
        for (unavailablePrice in listOf(null, Int.MIN_VALUE, -1, 0)) {
            composeRule.runOnIdle { price = unavailablePrice }
            composeRule.onNodeWithText("가격 정보 없음").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("0원").assertDoesNotExist()
            composeRule.onNodeWithText("1원").assertDoesNotExist()
        }
    }
}
