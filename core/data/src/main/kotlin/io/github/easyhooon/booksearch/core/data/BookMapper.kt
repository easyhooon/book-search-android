package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.BookIdentity
import io.github.easyhooon.booksearch.core.model.SearchPage
import io.github.easyhooon.booksearch.core.network.model.DocumentResponse
import io.github.easyhooon.booksearch.core.network.model.SearchBookResponse
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal fun SearchBookResponse.toSearchPage(page: Int): SearchPage = SearchPage(
    page = page,
    books = documents.map { it.toBook() },
    isEnd = meta.isEnd,
    totalCount = meta.totalCount,
)

internal fun DocumentResponse.toBook(): Book {
    val identity = BookIdentity.of(
        rawIsbn = isbn,
        url = url,
        title = title,
        publisher = publisher,
        rawDatetime = datetime,
    )
    return Book(
        id = identity.id,
        isbn10 = identity.isbn10,
        isbn13 = identity.isbn13,
        title = title.trim(),
        authors = authors.map { it.trim() }.filter { it.isNotEmpty() },
        translators = translators.map { it.trim() }.filter { it.isNotEmpty() },
        publisher = publisher.trim(),
        publishedDate = parsePublishedDate(datetime),
        // 원본 정가를 보존하고 표시·필터에서 0 이하를 가격 정보 없음으로 다룹니다.
        price = price,
        salePrice = salePrice?.takeIf { it >= 0 },
        thumbnailUrl = thumbnail.trim(),
        contents = contents.trim(),
        url = url.trim(),
        status = status.trim(),
    )
}

internal fun parsePublishedDate(raw: String): LocalDate? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    return try {
        OffsetDateTime.parse(value).toLocalDate()
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(value.take(10))
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
