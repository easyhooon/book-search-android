package io.github.easyhooon.booksearch.feature.search

import io.github.easyhooon.booksearch.core.data.FavoriteIdsSubscriptionKey
import io.github.easyhooon.booksearch.core.data.FavoriteMutationKeyFactory
import io.github.easyhooon.booksearch.core.data.SearchBooksQueryKeyFactory
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationKey

/**
 * 검색 엔트리 하나의 의존성 그래프입니다. 앱에서 조립하고 엔트리에서 retain하므로
 * 엔트리가 백스택에 있는 동안 같은 key 인스턴스를 씁니다.
 */
interface SearchScreenGraph {
    val searchScreenContext: SearchScreenContext

    fun interface Factory {
        fun createSearchScreenGraph(): SearchScreenGraph
    }
}

class SearchScreenContext(
    val searchBooksQueryKeyFactory: SearchBooksQueryKeyFactory,
    val favoriteIdsSubscriptionKey: FavoriteIdsSubscriptionKey,
    favoriteMutationKeyFactory: FavoriteMutationKeyFactory,
) {
    val favoriteMutationKey: MutationKey<Unit, FavoriteCommand> = favoriteMutationKeyFactory.create()
}
