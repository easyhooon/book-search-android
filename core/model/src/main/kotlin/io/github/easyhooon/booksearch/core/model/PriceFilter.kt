package io.github.easyhooon.booksearch.core.model

/**
 * 정가 기준 가격 범위 필터입니다. 양 끝값을 포함하고, null인 쪽은 제한하지 않습니다.
 *
 * 범위가 하나라도 지정되면 가격 정보가 없는 도서는 제외합니다.
 * 최솟값이 최댓값보다 큰 범위는 [isValid]가 false이며 적용하지 않습니다.
 */
data class PriceFilter(
    val min: Int? = null,
    val max: Int? = null,
) {
    val isActive: Boolean get() = min != null || max != null

    val isValid: Boolean get() = min == null || max == null || min <= max

    fun matches(price: Int?): Boolean {
        if (!isActive) return true
        if (price == null) return false
        if (min != null && price < min) return false
        if (max != null && price > max) return false
        return true
    }

    companion object {
        val None = PriceFilter()
    }
}
