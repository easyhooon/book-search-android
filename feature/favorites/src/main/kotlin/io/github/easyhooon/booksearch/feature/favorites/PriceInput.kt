package io.github.easyhooon.booksearch.feature.favorites

/** 가격 입력 칸의 규칙입니다. 숫자만 받고 Int 범위를 넘지 않도록 9자리로 자릅니다. */
internal object PriceInput {
    const val MAX_DIGITS = 9

    fun sanitize(text: String): String = text.filter { it in '0'..'9' }.take(MAX_DIGITS)

    /** 빈칸은 제한 없음(null)입니다. */
    fun parse(text: String): Int? = text.toIntOrNull()
}
