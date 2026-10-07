package io.github.easyhooon.booksearch.core.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationId
import soil.query.MutationKey
import soil.query.buildMutationKey

/**
 * 화면마다 다른 [screenTag]로 Mutation 상태를 분리합니다.
 * 반영 결과는 Room Subscription으로 모든 화면에 전달되므로 Mutation 데이터는 쓰지 않습니다.
 */
fun interface FavoriteMutationKeyFactory {
    fun create(screenTag: String): MutationKey<Unit, FavoriteCommand>
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultFavoriteMutationKeyFactory(
    private val writer: FavoriteWriter,
) : FavoriteMutationKeyFactory {
    override fun create(screenTag: String): MutationKey<Unit, FavoriteCommand> = buildMutationKey(
        id = MutationId("favorite", "screen" to screenTag),
        mutate = { command -> writer.write(command) },
    )
}
