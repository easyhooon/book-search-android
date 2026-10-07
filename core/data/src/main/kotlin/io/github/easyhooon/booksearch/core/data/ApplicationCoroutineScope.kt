package io.github.easyhooon.booksearch.core.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 화면 수명과 무관하게 끝까지 실행해야 하는 작업(즐겨찾기 쓰기)용 앱 범위 스코프입니다.
 * 하위 작업 하나의 실패가 다른 작업을 취소하지 않도록 SupervisorJob을 씁니다.
 */
class ApplicationCoroutineScope(scope: CoroutineScope) : CoroutineScope by scope

@ContributesTo(AppScope::class)
interface CoroutineBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun provideApplicationCoroutineScope(): ApplicationCoroutineScope =
        ApplicationCoroutineScope(CoroutineScope(SupervisorJob() + Dispatchers.Default))
}
