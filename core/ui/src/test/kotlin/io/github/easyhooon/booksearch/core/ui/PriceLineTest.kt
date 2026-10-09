package io.github.easyhooon.booksearch.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.ui.component.PriceLine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PriceLineTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `카드에서 양수 정가만 금액으로 표시하고 나머지는 가격 정보 없음으로 표시한다`() {
        var price by mutableStateOf<Int?>(1)
        composeRule.setContent {
            PriceLine(
                Book(
                    id = "a", isbn10 = null, isbn13 = null, title = "도서", authors = emptyList(),
                    translators = emptyList(), publisher = "", publishedDate = null, price = price,
                    salePrice = null, thumbnailUrl = "", contents = "", url = "", status = "",
                ),
            )
        }
        composeRule.onNodeWithText("1원").assertIsDisplayed()
        for (unavailablePrice in listOf(null, Int.MIN_VALUE, -1, 0)) {
            composeRule.runOnIdle { price = unavailablePrice }
            composeRule.onNodeWithText("가격 정보 없음").assertIsDisplayed()
            composeRule.onNodeWithText("0원").assertDoesNotExist()
            composeRule.onNodeWithText("1원").assertDoesNotExist()
        }
    }
}
