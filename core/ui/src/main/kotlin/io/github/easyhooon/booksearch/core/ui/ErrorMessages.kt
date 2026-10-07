package io.github.easyhooon.booksearch.core.ui

import androidx.annotation.StringRes

/**
 * core:data의 오류 분류를 화면 문구로 바꿀 때 쓰는 값입니다.
 * core:ui가 core:data에 의존하지 않도록 feature에서 분류 결과를 이 타입으로 옮깁니다.
 */
enum class ErrorMessage(@StringRes val resId: Int) {
    MissingApiKey(R.string.error_missing_api_key),
    Unauthorized(R.string.error_unauthorized),
    Network(R.string.error_network),
    Temporary(R.string.error_temporary),
    Generic(R.string.error_generic),
    FavoriteWrite(R.string.error_favorite_write),
}
