package io.github.easyhooon.booksearch.feature.search

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.easyhooon.booksearch.core.data.FavoriteIdsSubscriptionKey
import io.github.easyhooon.booksearch.core.data.FavoriteMutationKeyFactory
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationKey

abstract class SearchScreenScope private constructor()

/**
 * 검색 엔트리 하나의 의존성 그래프입니다. 구현은 Metro 컴파일러가 만들고,
 * 엔트리에서 retain하므로 엔트리가 백스택에 있는 동안 같은 key 인스턴스를 씁니다.
 */
@GraphExtension(SearchScreenScope::class)
interface SearchScreenGraph {
    val searchScreenContext: SearchScreenContext

    @GraphExtension.Factory
    @ContributesTo(AppScope::class)
    fun interface Factory {
        fun createSearchScreenGraph(): SearchScreenGraph
    }
}

@Inject
@SingleIn(SearchScreenScope::class)
class SearchScreenContext(
    val searchBooksQueryKeyFactory: SearchBooksQueryKeyFactory,
    val favoriteIdsSubscriptionKey: FavoriteIdsSubscriptionKey,
    favoriteMutationKeyFactory: FavoriteMutationKeyFactory,
) {
    val favoriteMutationKey: MutationKey<Unit, FavoriteCommand> = favoriteMutationKeyFactory.create("search")
}
