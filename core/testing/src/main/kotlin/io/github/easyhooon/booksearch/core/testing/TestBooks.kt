package io.github.easyhooon.booksearch.core.testing

import io.github.easyhooon.booksearch.core.model.Book
import java.time.LocalDate

fun testBook(
    id: String,
    title: String = "제목 $id",
    authors: List<String> = listOf("저자 $id"),
    price: Int? = 10_000,
    salePrice: Int? = null,
    publishedDate: LocalDate? = LocalDate.of(2024, 1, 1),
): Book = Book(
    id = id,
    isbn10 = null,
    isbn13 = id,
    title = title,
    authors = authors,
    translators = emptyList(),
    publisher = "출판사",
    publishedDate = publishedDate,
    price = price,
    salePrice = salePrice,
    thumbnailUrl = "",
    contents = "소개 $id",
    url = "",
    status = "정상판매",
)
