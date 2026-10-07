package io.github.easyhooon.booksearch.navigation

import androidx.navigation3.runtime.NavKey
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.feature.detail.BookDetailNavKey
import io.github.easyhooon.booksearch.feature.favorites.FavoritesNavKey
import io.github.easyhooon.booksearch.feature.search.SearchNavKey

/**
 * 백스택 규칙(SPEC 7절)입니다. 백스택은 항상 `[비활성 탭 루트(있으면), 활성 탭 루트, 상세…]` 형태입니다.
 * 두 탭 루트는 pop하지 않고 순서만 바꾸므로 목록 상태가 retain으로 유지됩니다.
 */
enum class TopLevelTab(val rootKey: NavKey) {
    Search(SearchNavKey),
    Favorites(FavoritesNavKey),
}

private fun NavKey.isTabRoot(): Boolean = TopLevelTab.entries.any { it.rootKey == this }

/** 활성 탭 루트의 위치입니다. 이 위치부터 끝까지가 화면에 보이는 스택입니다. */
val List<NavKey>.activeRootIndex: Int
    get() = indexOfLast { it.isTabRoot() }.also { check(it >= 0) { "탭 루트가 없는 백스택입니다." } }

val List<NavKey>.currentTab: TopLevelTab
    get() = this[activeRootIndex].let { root -> TopLevelTab.entries.first { it.rootKey == root } }

val List<NavKey>.isDetailOnTop: Boolean
    get() = lastOrNull() is BookDetailNavKey

/** 다른 탭이면 현재 탭의 상세를 정리하고 대상 루트를 맨 위로 옮깁니다. 같은 탭이면 루트까지 pop합니다. */
fun MutableList<NavKey>.selectTab(tab: TopLevelTab) {
    val rootIndex = activeRootIndex
    while (lastIndex > rootIndex) removeAt(lastIndex)
    if (last() == tab.rootKey) return
    remove(tab.rootKey)
    add(tab.rootKey)
}

/**
 * 상세는 목록에서만 엽니다. 1-pane에서는 상세 화면에 다른 도서로 가는 경로가 없으므로, 맨 위가 이미 상세면
 * 두 손가락으로 두 카드를 동시에 눌렀거나 전환 중에 다른 카드를 누른 경우로 보고 무시합니다.
 * 2-pane([replaceDetail])에서는 목록이 함께 보이므로 다른 도서를 누르면 상세를 교체합니다. 같은 도서면 무시합니다.
 * 어느 경우에도 상세는 하나만 쌓입니다.
 */
fun MutableList<NavKey>.openDetail(book: Book, replaceDetail: Boolean = false) {
    val top = lastOrNull()
    if (top is BookDetailNavKey) {
        if (!replaceDetail || top.book.id == book.id) return
        removeAt(lastIndex)
    }
    add(BookDetailNavKey(book))
}

/** 맨 위 상세의 도서 ID입니다. 2-pane에서 목록의 선택 표시에 씁니다. */
val List<NavKey>.openedBookId: String?
    get() = (lastOrNull() as? BookDetailNavKey)?.book?.id

/** 처리했으면 true, 검색 루트라서 앱을 종료해야 하면 false입니다. */
fun MutableList<NavKey>.navigateBack(): Boolean {
    when {
        isDetailOnTop -> removeAt(lastIndex)
        currentTab == TopLevelTab.Favorites -> selectTab(TopLevelTab.Search)
        else -> return false
    }
    return true
}
