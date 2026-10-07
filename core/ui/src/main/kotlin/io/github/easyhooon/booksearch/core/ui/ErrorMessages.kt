package io.github.easyhooon.booksearch.core.ui

import androidx.annotation.StringRes

/**
 * 화면에 보여 줄 오류 문구입니다. core:ui가 core:data에 의존하지 않도록
 * 오류 분류에서 이 값으로의 변환은 feature가 맡습니다.
 */
enum class ErrorMessage(@StringRes val resId: Int) {
    MissingApiKey(R.string.error_missing_api_key),
    Unauthorized(R.string.error_unauthorized),
    Network(R.string.error_network),
    Temporary(R.string.error_temporary),
    Generic(R.string.error_generic),
    FavoriteWrite(R.string.error_favorite_write),
}
