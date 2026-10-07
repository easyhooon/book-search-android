package io.github.easyhooon.booksearch.core.model

/**
 * API 응답의 `isbn` 문자열과 대체 재료로 만든 도서 식별 정보입니다.
 *
 * ID 우선순위는 `isbn13:` → `isbn10:` → `url:` → `fallback:{title}|{publisher}|{datetime}`입니다.
 * 같은 책이 응답마다 ISBN13/ISBN10 중 한쪽으로만 오면 서로 다른 ID가 되는 한계를 받아들입니다.
 */
data class BookIdentity(
    val id: String,
    val isbn10: String?,
    val isbn13: String?,
) {
    companion object {
        fun of(
            rawIsbn: String,
            url: String,
            title: String,
            publisher: String,
            rawDatetime: String,
        ): BookIdentity {
            val tokens = rawIsbn.split(' ')
                .map { token -> token.filter { it.isDigit() || it == 'X' || it == 'x' }.uppercase() }
                .filter { it.isNotEmpty() }
            val isbn13 = tokens.firstOrNull { it.length == 13 }
            val isbn10 = tokens.firstOrNull { it.length == 10 }
            val trimmedUrl = url.trim()
            val id = when {
                isbn13 != null -> "isbn13:$isbn13"
                isbn10 != null -> "isbn10:$isbn10"
                trimmedUrl.isNotEmpty() -> "url:$trimmedUrl"
                else -> "fallback:${title.trim()}|${publisher.trim()}|${rawDatetime.trim()}"
            }
            return BookIdentity(id = id, isbn10 = isbn10, isbn13 = isbn13)
        }
    }
}
