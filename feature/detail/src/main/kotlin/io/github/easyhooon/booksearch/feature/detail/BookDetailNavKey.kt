package io.github.easyhooon.booksearch.feature.detail

import androidx.navigation3.runtime.NavKey
import io.github.easyhooon.booksearch.core.model.Book
import kotlinx.serialization.Serializable

/**
 * 카드를 누른 시점의 도서 스냅샷을 담습니다. Kakao API에는 단건 조회가 없어서,
 * 프로세스 재생성 뒤에도 네트워크 없이 상세를 바로 그리기 위해 스냅샷을 백스택에 저장합니다.
 */
@Serializable
data class BookDetailNavKey(val book: Book) : NavKey
