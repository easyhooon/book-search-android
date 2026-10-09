package io.github.easyhooon.booksearch.core.data

import kotlinx.coroutines.CoroutineScope

/**
 * 화면 수명과 무관하게 끝까지 실행해야 하는 작업(즐겨찾기 쓰기)용 앱 범위 스코프입니다.
 * 하위 작업 하나의 실패가 다른 작업을 취소하지 않도록 SupervisorJob을 씁니다.
 */
class ApplicationCoroutineScope(scope: CoroutineScope) : CoroutineScope by scope
