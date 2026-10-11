package io.github.easyhooon.booksearch.core.data

import io.github.easyhooon.booksearch.core.database.FavoriteBookDao
import io.github.easyhooon.booksearch.core.database.toBook
import io.github.easyhooon.booksearch.core.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import soil.query.SubscriptionId
import soil.query.SubscriptionKey
import soil.query.SubscriptionOptionsOverride
import soil.query.SubscriptionReceiver
import soil.query.buildSubscriptionKey
import soil.query.copy
import kotlin.time.Duration

/** 검색 결과의 하트 표시에 쓰는 즐겨찾기 ID 집합입니다. Room이 단일 기준입니다. */
interface FavoriteIdsSubscriptionKey : SubscriptionKey<Set<String>>

/** 즐겨찾기 탭의 전체 목록입니다. 검색·필터·정렬은 화면에서 메모리로 처리합니다. */
interface FavoriteBooksSubscriptionKey : SubscriptionKey<List<Book>>

/** 상세 화면의 즐겨찾기 여부와 저장된 스냅샷입니다. 저장되지 않았으면 null입니다. */
fun interface FavoriteBookSubscriptionKeyFactory {
    fun create(id: String): SubscriptionKey<Book?>
}

class DefaultFavoriteIdsSubscriptionKey(
    dao: FavoriteBookDao,
) : FavoriteIdsSubscriptionKey,
    SubscriptionKey<Set<String>> by roomSubscriptionKey(
        id = SubscriptionId("favorite-ids"),
        subscribe = { dao.observeIds().map { it.toSet() }.distinctUntilChanged() },
    )

class DefaultFavoriteBooksSubscriptionKey(
    dao: FavoriteBookDao,
) : FavoriteBooksSubscriptionKey,
    SubscriptionKey<List<Book>> by roomSubscriptionKey(
        id = SubscriptionId("favorite-books"),
        subscribe = { dao.observeAll().map { entities -> entities.map { it.toBook() } }.distinctUntilChanged() },
    )

class DefaultFavoriteBookSubscriptionKeyFactory(
    private val dao: FavoriteBookDao,
) : FavoriteBookSubscriptionKeyFactory {
    override fun create(id: String): SubscriptionKey<Book?> = roomSubscriptionKey(
        id = SubscriptionId("favorite-book", "id" to id),
        subscribe = { dao.observeById(id).map { it?.toBook() }.distinctUntilChanged() },
    )
}

/**
 * Room Flow를 구독하는 key입니다. 화면이 구독을 놓으면 Flow 수집도 바로 멈춥니다.
 *
 * Soil은 구독자가 없어도 keepAliveTime(기본 5초) 동안 Flow를 계속 수집하지만 그 사이 값은 다시 전달하지 않습니다.
 * 상세에 들어간 직후 저장하고 돌아오면 목록이 저장 전 상태로 남으므로, 다시 구독할 때 Room이 현재 값부터 내보내게 합니다.
 */
private fun <T> roomSubscriptionKey(
    id: SubscriptionId<T>,
    subscribe: SubscriptionReceiver.() -> Flow<T>,
): SubscriptionKey<T> = object : SubscriptionKey<T> by buildSubscriptionKey(id = id, subscribe = subscribe) {
    override fun onConfigureOptions(): SubscriptionOptionsOverride = { it.copy(keepAliveTime = Duration.ZERO) }
}
