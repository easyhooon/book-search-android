package io.github.easyhooon.booksearch.core.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * 화면이 다루는 도서 모델입니다. 네트워크 DTO나 Room Entity와 분리되어 있으며,
 * 상세 화면 NavKey에 스냅샷으로 들어가므로 직렬화할 수 있어야 합니다.
 *
 * 정가는 원본 값을 보존합니다. 표시·필터에 쓸 수 있는 양수 정가는 [availablePrice]로 구분합니다.
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
    /** 앱의 방어적 정책: 누락되거나 0 이하인 정가는 가격 정보 없음으로 표시합니다. */
    val availablePrice: Int?
        get() = price?.takeIf { it > 0 }

    /** 정가 정보가 있고 판매가가 정가보다 낮을 때만 할인 판매가로 봅니다. */
    val discountedPrice: Int?
        get() = availablePrice?.let { regularPrice -> salePrice?.takeIf { it < regularPrice } }
}
