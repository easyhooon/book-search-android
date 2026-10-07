package io.github.easyhooon.booksearch.core.database

import io.github.easyhooon.booksearch.core.model.Book
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun FavoriteBookEntity.toBook(): Book = Book(
    id = id,
    isbn10 = isbn10,
    isbn13 = isbn13,
    title = title,
    authors = authors,
    translators = translators,
    publisher = publisher,
    publishedDate = publishedDate?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    },
    price = price,
    salePrice = salePrice,
    thumbnailUrl = thumbnailUrl,
    contents = contents,
    url = url,
    status = status,
)

fun Book.toFavoriteEntity(savedAt: Long): FavoriteBookEntity = FavoriteBookEntity(
    id = id,
    isbn10 = isbn10,
    isbn13 = isbn13,
    title = title,
    authors = authors,
    translators = translators,
    publisher = publisher,
    publishedDate = publishedDate?.toString(),
    price = price,
    salePrice = salePrice,
    thumbnailUrl = thumbnailUrl,
    contents = contents,
    url = url,
    status = status,
    savedAt = savedAt,
)
