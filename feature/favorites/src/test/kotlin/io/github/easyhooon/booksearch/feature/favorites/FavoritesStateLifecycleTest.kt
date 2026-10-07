package io.github.easyhooon.booksearch.feature.favorites

import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import io.github.easyhooon.booksearch.core.model.FavoriteSort
import io.github.easyhooon.booksearch.core.model.PriceFilter
import io.github.easyhooon.booksearch.core.testing.FakeFavoriteWriter
import io.github.easyhooon.booksearch.core.testing.testBook
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.compose.LocalMutationClient
import soil.query.compose.LocalQueryClient

/**
 * 즐겨찾기 Presenter의 저장 데이터와 retain 데이터가 각자의 수명을 따르는지 확인합니다.
 *
 * 저장 데이터는 프로세스 재생성(저장 후 복원)에서, 즐겨찾기 쓰기 진행은 회전처럼 컴포지션만 빠졌다 돌아올 때
 * 유지되어야 합니다. 엔트리가 pop되면 진행 상태는 버리지만 이미 시작한 쓰기는 앱 범위에서 끝납니다.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSoilQueryApi::class)
class FavoritesStateLifecycleTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = SwrCachePlus(clientScope)
    private val writer = FakeFavoriteWriter()
    private val book = testBook("k", price = 30_000)

    private var shown by mutableStateOf(true)
    private lateinit var registry: RetainedValuesStoreRegistry
    private lateinit var latest: FavoritesUiState

    @After
    fun tearDown() {
        clientScope.cancel()
    }

    @Composable
    private fun Presenter() {
        CompositionLocalProvider(LocalQueryClient provides client, LocalMutationClient provides client) {
            latest = favoritesPresenter(favoriteBooks = listOf(book), favoriteMutationKey = writer.key)
        }
    }

    /** NavDisplay처럼 엔트리별 saveable·retain 저장소 안에서 화면을 빼고 다시 넣을 수 있게 합니다. */
    private fun setEntryContent() {
        composeRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            registry = retainRetainedValuesStoreRegistry()
            if (shown) {
                saveableStateHolder.SaveableStateProvider(ENTRY) {
                    registry.LocalRetainedValuesStoreProvider(ENTRY) { Presenter() }
                }
            }
        }
    }

    private fun send(event: FavoritesEvent) {
        composeRule.runOnIdle { latest.eventSink(event) }
        composeRule.waitForIdle()
    }

    /** Soil의 Mutation 결과가 main looper에 올라오므로 Robolectric looper를 비웁니다. */
    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
    }

    private fun recompose(show: Boolean) {
        shown = show
        composeRule.waitForIdle()
    }

    @Test
    fun `프로세스 재생성 뒤에도 입력과 확정 조건을 복원한다`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { Presenter() }
        send(FavoritesEvent.InputChanged(" 코틀린 "))
        send(FavoritesEvent.Submit)
        send(FavoritesEvent.InputChanged("코틀린 인"))
        send(FavoritesEvent.SortSelected(FavoriteSort.TitleDescending))
        send(FavoritesEvent.PriceFilterApplied(PriceFilter(min = 10_000, max = 40_000)))

        restorationTester.emulateSavedInstanceStateRestore()

        assertEquals("코틀린 인", latest.input)
        assertEquals("코틀린", latest.submittedQuery)
        assertEquals(FavoriteSort.TitleDescending, latest.sort)
        assertEquals(PriceFilter(min = 10_000, max = 40_000), latest.priceFilter)
    }

    @Test
    fun `쓰기 중 컴포지션이 빠졌다 돌아와도 진행 상태를 유지하고 쓰기가 끝나면 푼다`() {
        val gate = CompletableDeferred<Unit>()
        writer.write = { gate.await() }
        setEntryContent()
        send(FavoritesEvent.ToggleFavorite(book))
        assertEquals(setOf(book.id), latest.pendingFavoriteIds)

        recompose(show = false)
        recompose(show = true)
        assertEquals(setOf(book.id), latest.pendingFavoriteIds)

        gate.complete(Unit)
        idleMainLooper()
        assertEquals(emptySet<String>(), latest.pendingFavoriteIds)
        assertNull(latest.userMessage)
        assertEquals(listOf(FavoriteCommand(book, favorite = false)), writer.commands)
    }

    @Test
    fun `엔트리가 pop되면 진행 상태를 버리고 다시 들어오면 새로 시작한다`() {
        val gate = CompletableDeferred<Unit>()
        writer.write = { gate.await() }
        setEntryContent()
        send(FavoritesEvent.ToggleFavorite(book))

        recompose(show = false)
        composeRule.runOnIdle { registry.clearChild(ENTRY) }
        recompose(show = true)
        assertEquals(emptySet<String>(), latest.pendingFavoriteIds)

        gate.complete(Unit)
        idleMainLooper()
        assertEquals(emptySet<String>(), latest.pendingFavoriteIds)
        assertNull(latest.userMessage)
        assertEquals(listOf(FavoriteCommand(book, favorite = false)), writer.commands)
    }

    private companion object {
        const val ENTRY = "favorites"
    }
}
