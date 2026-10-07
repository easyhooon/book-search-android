package io.github.easyhooon.booksearch.feature.detail

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.easyhooon.booksearch.core.data.FavoriteBookSubscriptionKeyFactory
import io.github.easyhooon.booksearch.core.data.FavoriteMutationKeyFactory
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationKey

abstract class BookDetailScreenScope private constructor()

/** 상세 엔트리 하나의 의존성 그래프입니다. 엔트리의 contentKey가 도서 ID이므로 도서별로 retain됩니다. */
@GraphExtension(BookDetailScreenScope::class)
interface BookDetailScreenGraph {
    val bookDetailScreenContext: BookDetailScreenContext

    @GraphExtension.Factory
    @ContributesTo(AppScope::class)
    fun interface Factory {
        fun createBookDetailScreenGraph(): BookDetailScreenGraph
    }
}

@Inject
@SingleIn(BookDetailScreenScope::class)
class BookDetailScreenContext(
    val favoriteBookSubscriptionKeyFactory: FavoriteBookSubscriptionKeyFactory,
    favoriteMutationKeyFactory: FavoriteMutationKeyFactory,
) {
    val favoriteMutationKey: MutationKey<Unit, FavoriteCommand> = favoriteMutationKeyFactory.create("detail")
}
