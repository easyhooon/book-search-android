package io.github.easyhooon.booksearch.feature.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.easyhooon.booksearch.core.model.Book
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.compose.SubscriptionSuccessObject
import soil.query.compose.rememberSubscription

/**
 * 상세 엔트리입니다. 같은 도서의 상세는 같은 retain 저장소를 쓰도록 contentKey를 도서 ID로 둡니다.
 * [metadata]는 2-pane 배치 역할로 app이 넘깁니다.
 */
fun EntryProviderScope<NavKey>.bookDetailEntry(
    graphFactory: BookDetailScreenGraph.Factory,
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) {
    entry<BookDetailNavKey>(clazzContentKey = { it.book.id }, metadata = metadata) { key ->
        val graph = retain { graphFactory.createBookDetailScreenGraph() }

        BookDetailScreenRoot(context = graph.bookDetailScreenContext, snapshot = key.book, onBack = onBack)
    }
}

@OptIn(ExperimentalSoilQueryApi::class)
@Composable
fun BookDetailScreenRoot(
    context: BookDetailScreenContext,
    snapshot: Book,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subscriptionKey = remember(snapshot.id) { context.favoriteBookSubscriptionKeyFactory.create(snapshot.id) }
    val saved = rememberSubscription(subscriptionKey)
    val uiState = bookDetailPresenter(
        snapshot = snapshot,
        roomBook = saved.data,
        isRoomLoaded = saved is SubscriptionSuccessObject,
        favoriteMutationKey = context.favoriteMutationKey,
    )

    BookDetailScreen(uiState = uiState, onBack = onBack, modifier = modifier)
}
