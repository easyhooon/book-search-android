package io.github.easyhooon.booksearch.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 즐겨찾기한 도서의 스냅샷입니다. 행이 존재하면 즐겨찾기 상태로 봅니다.
 * 발간일은 ISO-8601 문자열, 가격은 누락 시 null로 저장합니다.
 */
@Entity(tableName = "favorite_books")
data class FavoriteBookEntity(
    @PrimaryKey val id: String,
    val isbn10: String?,
    val isbn13: String?,
    val title: String,
    val authors: List<String>,
    val translators: List<String>,
    val publisher: String,
    @ColumnInfo(name = "published_date") val publishedDate: String?,
    val price: Int?,
    @ColumnInfo(name = "sale_price") val salePrice: Int?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    val contents: String,
    val url: String,
    val status: String,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)
