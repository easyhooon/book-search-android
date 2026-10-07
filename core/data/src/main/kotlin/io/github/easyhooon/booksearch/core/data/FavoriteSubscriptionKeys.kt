package io.github.easyhooon.booksearch.core.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.easyhooon.booksearch.core.database.FavoriteBookDao
import io.github.easyhooon.booksearch.core.database.toBook
import io.github.easyhooon.booksearch.core.model.Book
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import soil.query.SubscriptionId
import soil.query.SubscriptionKey
import soil.query.buildSubscriptionKey

/** 검색 결과의 하트 표시에 쓰는 즐겨찾기 ID 집합입니다. Room이 단일 기준입니다. */
interface FavoriteIdsSubscriptionKey : SubscriptionKey<Set<String>>

/** 즐겨찾기 탭의 전체 목록입니다. 검색·필터·정렬은 화면에서 메모리로 처리합니다. */
interface FavoriteBooksSubscriptionKey : SubscriptionKey<List<Book>>

/** 상세 화면의 즐겨찾기 여부와 저장된 스냅샷입니다. 저장되지 않았으면 null입니다. */
fun interface FavoriteBookSubscriptionKeyFactory {
    fun create(id: String): SubscriptionKey<Book?>
}

@ContributesBinding(AppScope::class, binding = binding<FavoriteIdsSubscriptionKey>())
@Inject
class DefaultFavoriteIdsSubscriptionKey(
    dao: FavoriteBookDao,
) : FavoriteIdsSubscriptionKey,
    SubscriptionKey<Set<String>> by buildSubscriptionKey(
        id = SubscriptionId("favorite-ids"),
        subscribe = { dao.observeIds().map { it.toSet() }.distinctUntilChanged() },
    )

@ContributesBinding(AppScope::class, binding = binding<FavoriteBooksSubscriptionKey>())
@Inject
class DefaultFavoriteBooksSubscriptionKey(
    dao: FavoriteBookDao,
) : FavoriteBooksSubscriptionKey,
    SubscriptionKey<List<Book>> by buildSubscriptionKey(
        id = SubscriptionId("favorite-books"),
        subscribe = { dao.observeAll().map { entities -> entities.map { it.toBook() } }.distinctUntilChanged() },
    )

@ContributesBinding(AppScope::class)
@Inject
class DefaultFavoriteBookSubscriptionKeyFactory(
    private val dao: FavoriteBookDao,
) : FavoriteBookSubscriptionKeyFactory {
    override fun create(id: String): SubscriptionKey<Book?> = buildSubscriptionKey(
        id = SubscriptionId("favorite-book", "id" to id),
        subscribe = { dao.observeById(id).map { it?.toBook() }.distinctUntilChanged() },
    )
}
