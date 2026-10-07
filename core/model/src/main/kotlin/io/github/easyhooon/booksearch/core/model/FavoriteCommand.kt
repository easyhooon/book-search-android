package io.github.easyhooon.booksearch.core.model

/**
 * 즐겨찾기 쓰기 명령입니다. 토글이 아니라 목표 상태를 담으므로 반복 실행해도 결과가 같습니다.
 */
data class FavoriteCommand(
    val book: Book,
    val favorite: Boolean,
)
