package io.github.easyhooon.booksearch.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.currentComposer
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import kotlinx.coroutines.flow.Flow
import soil.query.SwrClientPlus
import soil.query.compose.LocalMutationClient
import soil.query.compose.LocalQueryClient
import soil.query.compose.LocalSubscriptionClient
import soil.query.compose.LocalSwrClient

/**
 * Soil 클라이언트를 제공한 상태로 Presenter를 Molecule에서 실행합니다.
 *
 * Molecule은 바깥 본문이 다시 실행될 때만 값을 내보냅니다. `CompositionLocalProvider`의 content는
 * 따로 재구성될 수 있어 값이 누락되므로, DroidKaigi 2026의 PresenterTest처럼 provider를 직접
 * 열고 닫아 Presenter가 같은 그룹에서 실행되게 합니다.
 */
fun <T> presenterFlow(
    client: SwrClientPlus,
    presenter: @Composable () -> T,
): Flow<T> = moleculeFlow(RecompositionMode.Immediate) {
    provideWithReturnValue(
        LocalSwrClient provides client,
        LocalQueryClient provides client,
        LocalMutationClient provides client,
        LocalSubscriptionClient provides client,
    ) {
        presenter()
    }
}

@OptIn(InternalComposeApi::class)
@Composable
private fun <T> provideWithReturnValue(
    vararg values: ProvidedValue<*>,
    content: @Composable () -> T,
): T {
    currentComposer.startProviders(values)
    val result = content()
    currentComposer.endProviders()
    return result
}
