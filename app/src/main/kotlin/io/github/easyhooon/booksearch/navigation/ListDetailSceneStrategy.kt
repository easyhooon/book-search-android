package io.github.easyhooon.booksearch.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import io.github.easyhooon.booksearch.R

/** 창 너비가 expanded(840dp 이상)이면 목록과 상세를 나란히 보여 줍니다(SPEC 10절). */
@Composable
fun isListDetailTwoPane(): Boolean =
    currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

/** 엔트리가 2-pane에서 맡는 칸입니다. 엔트리 metadata로 표시합니다. */
enum class ListDetailRole { List, Detail }

private data object ListDetailRoleKey : NavMetadataKey<ListDetailRole>

object ListDetailMetadata {
    val list: Map<String, Any> = metadata { put(ListDetailRoleKey, ListDetailRole.List) }
    val detail: Map<String, Any> = metadata { put(ListDetailRoleKey, ListDetailRole.Detail) }
}

/**
 * 화면에 넘긴 엔트리가 `[목록]`이면 목록과 안내 칸을, `[목록, 상세]`이면 목록과 상세를 나란히 보여 주는 장면을 만듭니다.
 * 그 밖의 경우는 null을 돌려 기본 단일 장면에 맡깁니다. 2-pane이 아닐 때는 이 전략을 넘기지 않습니다.
 *
 * Material의 list-detail 전략은 navigation3-ui 1.0.0 기준이라 1.2.0의 [SceneStrategy]와 맞지 않아 직접 구현합니다.
 */
class ListDetailSceneStrategy : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val last = entries.last()
        return when (last.metadata[ListDetailRoleKey]) {
            ListDetailRole.List -> ListDetailScene(listEntry = last, detailEntry = null, previousEntries = entries.dropLast(1))
            ListDetailRole.Detail -> {
                val listEntry = entries.getOrNull(entries.lastIndex - 1)
                    ?.takeIf { it.metadata[ListDetailRoleKey] == ListDetailRole.List }
                    ?: return null
                ListDetailScene(listEntry = listEntry, detailEntry = last, previousEntries = entries.dropLast(1))
            }
            null -> null
        }
    }
}

/**
 * key를 목록 엔트리로 두어, 상세를 열고 닫거나 바꿔도 같은 장면으로 보고 목록을 다시 그리지 않습니다.
 * 상세 칸은 엔트리마다 composition을 새로 만들도록 contentKey로 감쌉니다.
 */
private class ListDetailScene(
    val listEntry: NavEntry<NavKey>,
    val detailEntry: NavEntry<NavKey>?,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val key: Any = listEntry.contentKey

    override val entries: List<NavEntry<NavKey>> = listOfNotNull(listEntry, detailEntry)

    override val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(ListPaneWidth)
                    .fillMaxHeight()
                    .testTag(ListDetailTestTags.LIST_PANE),
            ) {
                listEntry.Content()
            }
            VerticalDivider()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(ListDetailTestTags.DETAIL_PANE),
            ) {
                if (detailEntry != null) {
                    key(detailEntry.contentKey) { detailEntry.Content() }
                } else {
                    DetailPlaceholder()
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListDetailScene) return false
        return listEntry == other.listEntry &&
            detailEntry == other.detailEntry &&
            previousEntries == other.previousEntries
    }

    override fun hashCode(): Int {
        var result = listEntry.hashCode()
        result = 31 * result + (detailEntry?.hashCode() ?: 0)
        result = 31 * result + previousEntries.hashCode()
        return result
    }
}

@Composable
private fun DetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(ListDetailTestTags.PLACEHOLDER),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.detail_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 이 너비에서 목록 그리드는 1열입니다. */
private val ListPaneWidth = 400.dp

object ListDetailTestTags {
    const val LIST_PANE = "list-pane"
    const val DETAIL_PANE = "detail-pane"
    const val PLACEHOLDER = "detail-placeholder"
}
