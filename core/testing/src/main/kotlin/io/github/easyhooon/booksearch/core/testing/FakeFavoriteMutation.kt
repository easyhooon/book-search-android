package io.github.easyhooon.booksearch.core.testing

import io.github.easyhooon.booksearch.core.model.FavoriteCommand
import soil.query.MutationId
import soil.query.MutationKey
import soil.query.buildMutationKey

/** 즐겨찾기 명령을 기록하고, 테스트가 정한 대기나 실패를 돌려주는 Mutation key입니다. */
class FakeFavoriteMutation {
    val commands = mutableListOf<FavoriteCommand>()
    var write: suspend (FavoriteCommand) -> Unit = {}

    val key: MutationKey<Unit, FavoriteCommand> = buildMutationKey(
        id = MutationId("favorite", "screen" to "test"),
        mutate = { command ->
            commands += command
            write(command)
        },
    )
}
