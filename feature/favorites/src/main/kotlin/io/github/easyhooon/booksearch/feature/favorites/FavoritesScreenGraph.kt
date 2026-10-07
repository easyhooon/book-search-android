package io.github.easyhooon.booksearch.feature.favorites

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.easyhooon.booksearch.core.data.FavoriteBooksSubscriptionKey
import io.github.easyhooon.booksearch.core.data.FavoriteMutationKeyFactory
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationKey

abstract class FavoritesScreenScope private constructor()

/** 즐겨찾기 엔트리 하나의 의존성 그래프입니다. 엔트리에서 retain합니다. */
@GraphExtension(FavoritesScreenScope::class)
interface FavoritesScreenGraph {
    val favoritesScreenContext: FavoritesScreenContext

    @GraphExtension.Factory
    @ContributesTo(AppScope::class)
    fun interface Factory {
        fun createFavoritesScreenGraph(): FavoritesScreenGraph
    }
}

@Inject
@SingleIn(FavoritesScreenScope::class)
class FavoritesScreenContext(
    val favoriteBooksSubscriptionKey: FavoriteBooksSubscriptionKey,
    favoriteMutationKeyFactory: FavoriteMutationKeyFactory,
) {
    val favoriteMutationKey: MutationKey<Unit, FavoriteCommand> = favoriteMutationKeyFactory.create("favorites")
}
