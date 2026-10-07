package io.github.easyhooon.booksearch.core.model

fun testBook(
    id: String,
    title: String = "제목 $id",
    authors: List<String> = listOf("저자"),
    price: Int? = 10_000,
    salePrice: Int? = null,
) = Book(
    id = id,
    isbn10 = null,
    isbn13 = null,
    title = title,
    authors = authors,
    translators = emptyList(),
    publisher = "출판사",
    publishedDate = null,
    price = price,
    salePrice = salePrice,
    thumbnailUrl = "",
    contents = "",
    url = "",
    status = "",
)
