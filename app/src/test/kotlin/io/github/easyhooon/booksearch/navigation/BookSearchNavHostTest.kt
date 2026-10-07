package io.github.easyhooon.booksearch.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.test.core.app.ActivityScenario
import io.github.easyhooon.booksearch.core.testing.testBook
import io.github.easyhooon.booksearch.feature.detail.BookDetailNavKey
import io.github.easyhooon.booksearch.feature.favorites.FavoritesNavKey
import io.github.easyhooon.booksearch.feature.search.SearchNavKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 실제 화면 대신 retain 값의 생성·은퇴를 기록하는 가짜 엔트리로 NavHost의 백스택·retain 수명·탐색 UI를 확인합니다.
 */
@RunWith(RobolectricTestRunner::class)
class BookSearchNavHostTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val created = mutableListOf<String>()
    private val retired = mutableListOf<String>()
    private lateinit var backStack: NavBackStack<NavKey>
    private lateinit var scenario: ActivityScenario<ComponentActivity>

    private inner class Tracked(private val name: String) : RetainObserver {
        override fun onRetained() {
            created += name
        }

        override fun onEnteredComposition() = Unit
        override fun onExitedComposition() = Unit
        override fun onRetired() {
            retired += name
        }

        override fun onUnused() = Unit
    }

    @Composable
    private fun TrackedScreen(
        name: String,
        onOpenOther: (() -> Unit)? = null,
        onOpenDetail: (() -> Unit)? = null,
    ) {
        retain(name) { Tracked(name) }
        Column(Modifier.testTag(name)) {
            Text(name)
            if (onOpenDetail != null) {
                Button(onClick = onOpenDetail, modifier = Modifier.testTag("$name-open")) { Text("open") }
            }
            if (onOpenOther != null) {
                Button(onClick = onOpenOther, modifier = Modifier.testTag("$name-open-other")) { Text("other") }
            }
        }
    }

    private fun setContent() = scenario.onActivity { activity ->
        activity.setContent {
            MaterialTheme {
                val backStack = rememberNavBackStack(SearchNavKey).also { backStack = it }
                val isTwoPane by rememberUpdatedState(isListDetailTwoPane())
                fun open(id: String) = backStack.openDetail(testBook(id), replaceDetail = isTwoPane)
                BookSearchNavHost(
                    backStack = backStack,
                    entryProvider = entryProvider<NavKey> {
                        entry<SearchNavKey>(metadata = ListDetailMetadata.list) {
                            TrackedScreen(name = "search", onOpenOther = { open("c") }) { open("a") }
                        }
                        entry<FavoritesNavKey>(metadata = ListDetailMetadata.list) { TrackedScreen("favorites") { open("b") } }
                        entry<BookDetailNavKey>(clazzContentKey = { it.book.id }, metadata = ListDetailMetadata.detail) {
                            TrackedScreen("detail-${it.book.id}")
                        }
                    },
                )
            }
        }
    }

    private fun launch() {
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        setContent()
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun clickTab(tab: TopLevelTab) {
        composeRule.onNodeWithTag(NavHostTestTags.tab(tab)).performClick()
        composeRule.waitForIdle()
    }

    private fun openDetailFrom(screen: String) {
        composeRule.onNodeWithTag("$screen-open").performClick()
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun `탭을 오가도 두 탭 루트의 retain 값은 은퇴하지 않고 다시 쓰인다`() {
        launch()
        clickTab(TopLevelTab.Favorites)
        composeRule.onNodeWithTag("favorites").assertIsDisplayed()
        clickTab(TopLevelTab.Search)
        clickTab(TopLevelTab.Favorites)
        clickTab(TopLevelTab.Search)

        composeRule.onNodeWithTag("search").assertIsDisplayed()
        assertEquals(listOf("search", "favorites"), created)
        assertEquals(emptyList<String>(), retired)
    }

    @Test
    fun `두 카드를 두 손가락으로 동시에 누르면 먼저 뗀 쪽의 상세 하나만 쌓인다`() {
        launch()
        val first = composeRule.onNodeWithTag("search-open").fetchSemanticsNode().boundsInRoot.center
        val second = composeRule.onNodeWithTag("search-open-other").fetchSemanticsNode().boundsInRoot.center

        composeRule.onRoot().performTouchInput {
            down(0, first)
            down(1, second)
            up(0)
            up(1)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(SearchNavKey, BookDetailNavKey(testBook("a"))), backStack.toList())
        assertEquals(listOf("search", "detail-a"), created)
    }

    @Test
    fun `상세를 뒤로 가기로 닫으면 상세의 retain 값만 은퇴한다`() {
        launch()
        openDetailFrom("search")
        composeRule.onNodeWithTag("detail-a").assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag("search").assertIsDisplayed()
        assertEquals(listOf("search", "detail-a"), created)
        assertEquals(listOf("detail-a"), retired)
    }

    /** 상세에서는 탭 UI가 숨겨지므로, 탭 루트를 옮기면서 상세를 pop하는 한 번의 변경을 직접 적용합니다. */
    @Test
    fun `상세가 열린 채 다른 탭으로 가면 상세만 정리된다`() {
        launch()
        openDetailFrom("search")

        composeRule.runOnIdle { backStack.selectTab(TopLevelTab.Favorites) }
        composeRule.onNodeWithTag("favorites").assertIsDisplayed()
        clickTab(TopLevelTab.Search)

        composeRule.onNodeWithTag("search").assertIsDisplayed()
        assertEquals(listOf("detail-a"), retired)
        assertEquals(1, created.count { it == "search" })
    }

    @Test
    fun `상세에서는 탐색 UI를 숨기고 돌아오면 다시 보인다`() {
        launch()
        composeRule.onNodeWithTag(NavHostTestTags.tab(TopLevelTab.Search)).assertIsDisplayed()

        openDetailFrom("search")
        composeRule.onNodeWithTag(NavHostTestTags.tab(TopLevelTab.Search)).assertDoesNotExist()

        pressBack()
        composeRule.onNodeWithTag(NavHostTestTags.tab(TopLevelTab.Search)).assertIsDisplayed()
    }

    @Test
    fun `즐겨찾기 루트에서 뒤로 가면 검색 탭으로 가고 검색 루트에서는 종료한다`() {
        launch()
        clickTab(TopLevelTab.Favorites)

        pressBack()
        composeRule.onNodeWithTag("search").assertIsDisplayed()
        assertEquals(listOf(FavoritesNavKey, SearchNavKey), backStack.toList())
        assertEquals(emptyList<String>(), retired)

        pressBack()
        scenario.onActivity { assertTrue(it.isFinishing) }
    }

    @Test
    fun `구성 변경으로 Activity가 다시 만들어져도 백스택과 retain 값이 유지된다`() {
        launch()
        clickTab(TopLevelTab.Favorites)
        openDetailFrom("favorites")

        scenario.recreate()
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-b").assertIsDisplayed()
        assertEquals(listOf("search", "favorites", "detail-b"), created)
        assertEquals(emptyList<String>(), retired)

        pressBack()
        composeRule.onNodeWithTag("favorites").assertIsDisplayed()
        assertEquals(listOf("detail-b"), retired)
    }

    @Test
    fun `좁은 창에서는 하단 바로 탭을 보여 준다`() {
        launch()
        val root = composeRule.onRoot().getBoundsInRoot()
        val tab = composeRule.onNodeWithTag(NavHostTestTags.tab(TopLevelTab.Search)).getBoundsInRoot()

        assertTrue("하단에 있어야 합니다: $tab", tab.top > root.bottom / 2)
    }

    @Test
    @Config(qualifiers = "w1000dp-h700dp")
    fun `넓은 창에서는 내비게이션 레일로 탭을 보여 준다`() {
        launch()
        val root = composeRule.onRoot().getBoundsInRoot()
        val tab = composeRule.onNodeWithTag(NavHostTestTags.tab(TopLevelTab.Search)).getBoundsInRoot()

        assertTrue("왼쪽에 있어야 합니다: $tab", tab.right < root.right / 4)
        assertTrue("하단 바가 아니어야 합니다: $tab", tab.top < root.bottom / 2)
    }

    @Test
    @Config(qualifiers = WIDE)
    fun `넓은 창에서는 목록 옆에 안내 칸을 두고 상세를 열면 레일을 유지한 채 오른쪽에 보여 준다`() {
        launch()
        composeRule.onNodeWithTag("search").assertIsDisplayed()
        composeRule.onNodeWithTag(ListDetailTestTags.PLACEHOLDER).assertIsDisplayed()

        openDetailFrom("search")

        composeRule.onNodeWithTag("search").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-a").assertIsDisplayed()
        composeRule.onNodeWithTag(ListDetailTestTags.PLACEHOLDER).assertDoesNotExist()
        composeRule.onNodeWithTag(NavHostTestTags.tab(TopLevelTab.Search)).assertIsDisplayed()
        val list = composeRule.onNodeWithTag("search").getBoundsInRoot()
        val detail = composeRule.onNodeWithTag("detail-a").getBoundsInRoot()
        assertTrue("상세가 목록 오른쪽에 있어야 합니다: $list, $detail", detail.left >= list.right)
    }

    @Test
    @Config(qualifiers = WIDE)
    fun `넓은 창에서 다른 도서를 고르면 상세를 교체하고 이전 상세의 retain 값만 은퇴한다`() {
        launch()
        openDetailFrom("search")
        composeRule.onNodeWithTag("search-open-other").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("detail-c").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-a").assertDoesNotExist()
        assertEquals(listOf(SearchNavKey, BookDetailNavKey(testBook("c"))), backStack.toList())
        assertEquals(listOf("search", "detail-a", "detail-c"), created)
        assertEquals(listOf("detail-a"), retired)
    }

    @Test
    @Config(qualifiers = WIDE)
    fun `넓은 창에서 뒤로 가기로 상세를 닫으면 안내 칸으로 돌아가고 검색 루트에서는 종료한다`() {
        launch()
        openDetailFrom("search")

        pressBack()
        composeRule.onNodeWithTag(ListDetailTestTags.PLACEHOLDER).assertIsDisplayed()
        composeRule.onNodeWithTag("search").assertIsDisplayed()
        assertEquals(listOf("detail-a"), retired)

        pressBack()
        scenario.onActivity { assertTrue(it.isFinishing) }
    }

    @Test
    @Config(qualifiers = WIDE)
    fun `창 너비가 바뀌면 같은 백스택으로 2-pane과 1-pane을 오가고 retain 값은 유지된다`() {
        launch()
        clickTab(TopLevelTab.Favorites)
        openDetailFrom("favorites")

        RuntimeEnvironment.setQualifiers(NARROW)
        scenario.recreate()
        setContent()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail-b").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites").assertDoesNotExist()
        composeRule.onNodeWithTag(NavHostTestTags.tab(TopLevelTab.Search)).assertDoesNotExist()

        RuntimeEnvironment.setQualifiers(WIDE)
        scenario.recreate()
        setContent()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("detail-b").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites").assertIsDisplayed()

        assertEquals(listOf(SearchNavKey, FavoritesNavKey, BookDetailNavKey(testBook("b"))), backStack.toList())
        assertEquals(listOf("search", "favorites", "detail-b"), created)
        assertEquals(emptyList<String>(), retired)
    }

    private companion object {
        const val WIDE = "w1280dp-h800dp"
        const val NARROW = "w600dp-h800dp"
    }
}
