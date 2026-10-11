package io.github.easyhooon.booksearch.core.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.easyhooon.booksearch.core.database.FavoriteBookDao
import io.github.easyhooon.booksearch.core.database.toFavoriteEntity
import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationId
import soil.query.MutationKey
import soil.query.buildMutationKey

/**
 * 모든 화면의 즐겨찾기 명령을 하나의 Mutation actor에서 접수 순서대로 실행합니다.
 * 진행·실패 표시는 화면별 Toggler가 소유하고, 반영 결과는 Room Subscription으로 전달합니다.
 */
fun interface FavoriteMutationKeyFactory {
    fun create(): MutationKey<Unit, FavoriteCommand>
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultFavoriteMutationKeyFactory(
    private val dao: FavoriteBookDao,
) : FavoriteMutationKeyFactory {
    override fun create(): MutationKey<Unit, FavoriteCommand> = buildMutationKey(
        id = MutationId("favorite"),
        mutate = { command ->
            if (command.favorite) {
                dao.upsert(command.book.toFavoriteEntity(savedAt = System.currentTimeMillis()))
            } else {
                dao.deleteById(command.book.id)
            }
        },
    )
}
