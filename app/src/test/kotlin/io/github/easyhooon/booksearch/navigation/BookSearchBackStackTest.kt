package io.github.easyhooon.booksearch.navigation

import androidx.navigation3.runtime.NavKey
import io.github.easyhooon.booksearch.core.testing.testBook
import io.github.easyhooon.booksearch.feature.detail.BookDetailNavKey
import io.github.easyhooon.booksearch.feature.favorites.FavoritesNavKey
import io.github.easyhooon.booksearch.feature.search.SearchNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSearchBackStackTest {

    private val bookA = testBook("a")
    private val bookB = testBook("b")

    private fun stackOf(vararg keys: NavKey): MutableList<NavKey> = mutableListOf(*keys)

    @Test
    fun `다른 탭을 고르면 대상 루트를 맨 위로 옮기고 처음이면 추가한다`() {
        val stack = stackOf(SearchNavKey)

        stack.selectTab(TopLevelTab.Favorites)
        assertEquals(listOf(SearchNavKey, FavoritesNavKey), stack)
        assertEquals(TopLevelTab.Favorites, stack.currentTab)

        stack.selectTab(TopLevelTab.Search)
        assertEquals(listOf(FavoritesNavKey, SearchNavKey), stack)
        assertEquals(TopLevelTab.Search, stack.currentTab)
    }

    @Test
    fun `다른 탭을 고르면 현재 탭의 상세를 모두 정리한다`() {
        val stack = stackOf(FavoritesNavKey, SearchNavKey, BookDetailNavKey(bookA))

        stack.selectTab(TopLevelTab.Favorites)

        assertEquals(listOf(SearchNavKey, FavoritesNavKey), stack)
    }

    @Test
    fun `같은 탭을 다시 고르면 루트까지 pop하고 루트에서는 아무것도 하지 않는다`() {
        val stack = stackOf(FavoritesNavKey, SearchNavKey, BookDetailNavKey(bookA))

        stack.selectTab(TopLevelTab.Search)
        assertEquals(listOf(FavoritesNavKey, SearchNavKey), stack)

        stack.selectTab(TopLevelTab.Search)
        assertEquals(listOf(FavoritesNavKey, SearchNavKey), stack)
    }

    @Test
    fun `맨 위가 상세면 같은 도서든 다른 도서든 다시 쌓지 않는다`() {
        val stack = stackOf(SearchNavKey)

        stack.openDetail(bookA)
        stack.openDetail(bookA)
        assertEquals(listOf(SearchNavKey, BookDetailNavKey(bookA)), stack)

        // 두 카드를 동시에 누른 경우입니다.
        stack.openDetail(bookB)
        assertEquals(listOf(SearchNavKey, BookDetailNavKey(bookA)), stack)
        assertTrue(stack.isDetailOnTop)

        stack.navigateBack()
        stack.openDetail(bookB)
        assertEquals(listOf(SearchNavKey, BookDetailNavKey(bookB)), stack)
    }

    @Test
    fun `뒤로 가기는 상세를 pop하고 즐겨찾기 루트에서는 검색 탭으로 간다`() {
        val stack = stackOf(SearchNavKey, FavoritesNavKey, BookDetailNavKey(bookA))

        assertTrue(stack.navigateBack())
        assertEquals(listOf(SearchNavKey, FavoritesNavKey), stack)

        assertTrue(stack.navigateBack())
        assertEquals(listOf(FavoritesNavKey, SearchNavKey), stack)
    }

    @Test
    fun `검색 루트에서 뒤로 가기는 처리하지 않고 종료를 알린다`() {
        val stack = stackOf(FavoritesNavKey, SearchNavKey)

        assertFalse(stack.navigateBack())
        assertEquals(listOf(FavoritesNavKey, SearchNavKey), stack)
        assertEquals(1, stack.activeRootIndex)
    }
}
