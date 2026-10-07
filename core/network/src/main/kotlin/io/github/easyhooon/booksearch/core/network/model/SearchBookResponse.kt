package io.github.easyhooon.booksearch.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 응답 필드가 빠져도 화면 모델 변환에서 대체 값을 쓰도록 모든 필드에 기본값을 둡니다. */
@Serializable
data class SearchBookResponse(
    val meta: MetaResponse = MetaResponse(),
    val documents: List<DocumentResponse> = emptyList(),
)

@Serializable
data class MetaResponse(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("pageable_count") val pageableCount: Int = 0,
    @SerialName("is_end") val isEnd: Boolean = true,
)

@Serializable
data class DocumentResponse(
    val title: String = "",
    val contents: String = "",
    val url: String = "",
    val isbn: String = "",
    val datetime: String = "",
    val authors: List<String> = emptyList(),
    val publisher: String = "",
    val translators: List<String> = emptyList(),
    val price: Int? = null,
    @SerialName("sale_price") val salePrice: Int? = null,
    val thumbnail: String = "",
    val status: String = "",
)
