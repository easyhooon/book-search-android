package io.github.easyhooon.booksearch.feature.favorites

import io.github.easyhooon.booksearch.core.data.FavoriteBooksSubscriptionKey
import io.github.easyhooon.booksearch.core.data.FavoriteMutationKeyFactory
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationKey

/** 즐겨찾기 엔트리 하나의 의존성 그래프입니다. 엔트리에서 retain합니다. */
interface FavoritesScreenGraph {
    val favoritesScreenContext: FavoritesScreenContext

    fun interface Factory {
        fun createFavoritesScreenGraph(): FavoritesScreenGraph
    }
}

class FavoritesScreenContext(
    val favoriteBooksSubscriptionKey: FavoriteBooksSubscriptionKey,
    favoriteMutationKeyFactory: FavoriteMutationKeyFactory,
) {
    val favoriteMutationKey: MutationKey<Unit, FavoriteCommand> = favoriteMutationKeyFactory.create()
}
