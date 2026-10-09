package io.github.easyhooon.booksearch.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BookPricePolicyTest {
    @Test
    fun `정가가 없거나 0 이하이면 판매가가 있어도 할인 가격을 표시하지 않는다`() {
        for (price in listOf(null, Int.MIN_VALUE, -1, 0)) {
            val book = testBook("a", price = price, salePrice = 0)
            assertNull(book.availablePrice)
            assertNull(book.discountedPrice)
            assertEquals(price, book.price)
        }
    }

    @Test
    fun `양수 정가와 기존 0 판매가 할인 정책을 유지한다`() {
        assertEquals(1, testBook("a", price = 1).availablePrice)
        assertEquals(Int.MAX_VALUE, testBook("a", price = Int.MAX_VALUE).availablePrice)
        assertEquals(0, testBook("a", price = 1, salePrice = 0).discountedPrice)
    }

    @Test
    fun `상세 스냅샷 직렬화는 원본 정가를 보존하고 복원 후에도 같은 정책을 적용한다`() {
        for (price in listOf(null, Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE)) {
            val original = testBook("a", price = price)
            val encoded = Json.encodeToString(original)
            val restored = Json.decodeFromString<Book>(encoded)
            assertFalse(encoded.contains("availablePrice"))
            assertEquals(original, restored)
            assertEquals(price, restored.price)
            assertEquals(price?.takeIf { it > 0 }, restored.availablePrice)
        }
    }
}
