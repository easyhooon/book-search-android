package io.github.easyhooon.booksearch.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import io.github.easyhooon.booksearch.R

/**
 * 탭 UI와 NavDisplay를 조립합니다.
 *
 * 엔트리 장식(saveable·retain)은 백스택 전체에 적용하고, NavDisplay에는 활성 탭 루트부터의 엔트리만
 * 넘깁니다. 비활성 탭 루트는 백스택에 남아 있으므로 pop으로 처리되지 않아 상태가 유지되고,
 * 검색 루트에서 뒤로 가기를 미리 볼 때 다른 탭이 뒤에 비쳐 보이지도 않습니다.
 *
 * 화면에 엔트리가 하나뿐이면 NavDisplay가 뒤로 가기를 받지 않으므로, 검색 루트에서는 시스템이 Activity를 종료합니다.
 *
 * [isTwoPane]이면 [ListDetailSceneStrategy]로 목록과 상세를 나란히 보여 주고, 목록이 보이므로 상세에서도 탐색 UI를 유지합니다.
 */
@Composable
fun BookSearchNavHost(
    backStack: NavBackStack<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
    isTwoPane: Boolean = isListDetailTwoPane(),
) {
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberRetainedValuesStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
    val activeRootIndex = backStack.activeRootIndex
    val visibleEntries = entries.drop(activeRootIndex)
    val currentTab = backStack.currentTab

    // 화면에 엔트리가 하나뿐이면 NavDisplay가 뒤로 가기를 받지 않으므로 즐겨찾기 루트만 직접 처리합니다.
    BackHandler(enabled = visibleEntries.size == 1 && currentTab == TopLevelTab.Favorites) {
        backStack.navigateBack()
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = if (backStack.isDetailOnTop && !isTwoPane) {
            NavigationSuiteType.None
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
        },
        navigationSuiteItems = {
            TopLevelTab.entries.forEach { tab ->
                item(
                    selected = tab == currentTab,
                    onClick = { backStack.selectTab(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.labelResId)) },
                    modifier = Modifier.testTag(NavHostTestTags.tab(tab)),
                )
            }
        },
    ) {
        val sceneStrategy = remember(isTwoPane) {
            if (isTwoPane) ListDetailSceneStrategy() else SinglePaneSceneStrategy()
        }
        NavDisplay(
            entries = visibleEntries,
            sceneStrategies = listOf(sceneStrategy),
            onBack = { backStack.navigateBack() },
        )
    }
}

private val TopLevelTab.icon
    get() = when (this) {
        TopLevelTab.Search -> Icons.Filled.Search
        TopLevelTab.Favorites -> Icons.Filled.Favorite
    }

private val TopLevelTab.labelResId
    get() = when (this) {
        TopLevelTab.Search -> R.string.tab_search
        TopLevelTab.Favorites -> R.string.tab_favorites
    }

object NavHostTestTags {
    fun tab(tab: TopLevelTab) = "tab-${tab.name}"
}
