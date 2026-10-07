package io.github.easyhooon.booksearch.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.navigation3.runtime.NavEntryDecorator

/**
 * 엔트리마다 contentKey 단위의 retain 저장소를 제공합니다. Navigation 3 1.2.0에는 이 decorator가 없어
 * compose-runtime-retain의 [RetainedValuesStoreRegistry]로 직접 연결합니다.
 *
 * - 엔트리가 화면에서만 빠질 때(다른 탭으로 이동, 위에 상세가 쌓임)는 retain 값이 유지됩니다.
 * - 엔트리가 백스택에서 제거되면 Navigation 3가 onPop을 호출하고, 그때 저장소를 비워 값을 은퇴시킵니다.
 * - 레지스트리 자체가 retain되므로 회전 같은 구성 변경에서도 유지됩니다.
 */
@Composable
fun <T : Any> rememberRetainedValuesStoreNavEntryDecorator(
    registry: RetainedValuesStoreRegistry = retainRetainedValuesStoreRegistry(),
): NavEntryDecorator<T> = remember(registry) {
    NavEntryDecorator(
        onPop = { contentKey -> registry.clearChild(contentKey) },
        decorate = { entry ->
            registry.LocalRetainedValuesStoreProvider(entry.contentKey) {
                entry.Content()
            }
        },
    )
}
