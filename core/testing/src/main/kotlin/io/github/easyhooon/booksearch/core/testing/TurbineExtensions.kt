package io.github.easyhooon.booksearch.core.testing

import app.cash.turbine.ReceiveTurbine

/**
 * 조건에 맞는 상태가 나올 때까지 중간 방출을 건너뜁니다.
 * UiState에 이벤트 람다가 있어 매 재구성마다 새 값이 나오므로, 중간 상태 개수에 기대지 않습니다.
 */
suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
