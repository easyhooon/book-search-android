package io.github.easyhooon.booksearch.core.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * 화면이 다루는 도서 모델입니다. 네트워크 DTO나 Room Entity와 분리되어 있으며,
 * 상세 화면 NavKey에 스냅샷으로 들어가므로 직렬화할 수 있어야 합니다.
 *
 * 가격은 누락·음수면 null이고 0은 유효한 값입니다.
 */
@Serializable
data class Book(
    val id: String,
    val isbn10: String?,
    val isbn13: String?,
    val title: String,
    val authors: List<String>,
    val translators: List<String>,
    val publisher: String,
    @Serializable(with = LocalDateIsoSerializer::class)
    val publishedDate: LocalDate?,
    val price: Int?,
    val salePrice: Int?,
    val thumbnailUrl: String,
    val contents: String,
    val url: String,
    val status: String,
) {
    /** 판매가가 유효하고 정가보다 낮을 때만 할인 판매가로 봅니다. */
    val discountedPrice: Int?
        get() = if (price != null && salePrice != null && salePrice < price) salePrice else null
}
