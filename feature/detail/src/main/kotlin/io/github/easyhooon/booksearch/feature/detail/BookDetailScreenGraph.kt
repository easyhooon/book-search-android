package io.github.easyhooon.booksearch.feature.detail

import io.github.easyhooon.booksearch.core.data.FavoriteBookSubscriptionKeyFactory
import io.github.easyhooon.booksearch.core.data.FavoriteMutationKeyFactory
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationKey

/** 상세 엔트리 하나의 의존성 그래프입니다. 엔트리의 contentKey가 도서 ID이므로 도서별로 retain됩니다. */
interface BookDetailScreenGraph {
    val bookDetailScreenContext: BookDetailScreenContext

    fun interface Factory {
        fun createBookDetailScreenGraph(): BookDetailScreenGraph
    }
}

class BookDetailScreenContext(
    val favoriteBookSubscriptionKeyFactory: FavoriteBookSubscriptionKeyFactory,
    favoriteMutationKeyFactory: FavoriteMutationKeyFactory,
) {
    val favoriteMutationKey: MutationKey<Unit, FavoriteCommand> = favoriteMutationKeyFactory.create()
}
