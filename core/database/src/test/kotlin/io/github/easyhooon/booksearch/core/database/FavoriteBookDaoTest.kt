package io.github.easyhooon.booksearch.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.github.easyhooon.booksearch.core.model.Book
import io.github.easyhooon.booksearch.core.model.FavoriteListRules
import io.github.easyhooon.booksearch.core.model.FavoriteSort
import io.github.easyhooon.booksearch.core.model.PriceFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class FavoriteBookDaoTest {

    private lateinit var database: BookSearchDatabase
    private lateinit var dao: FavoriteBookDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BookSearchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.favoriteBookDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun book(id: String, title: String = "제목") = Book(
        id = id,
        isbn10 = "8996991341",
        isbn13 = null,
        title = title,
        authors = listOf("저자1", "저자, 2"),
        translators = emptyList(),
        publisher = "출판사",
        publishedDate = LocalDate.of(2024, 1, 31),
        price = 0,
        salePrice = null,
        thumbnailUrl = "",
        contents = "",
        url = "https://example.com",
        status = "정상판매",
    )

    @Test
    fun `저장한 도서를 같은 값으로 읽고 삭제하면 사라진다`() = runTest {
        dao.observeById("isbn10:8996991341").test {
            assertNull(awaitItem())

            dao.upsert(book("isbn10:8996991341").toFavoriteEntity(savedAt = 1L))
            assertEquals(book("isbn10:8996991341"), awaitItem()?.toBook())

            dao.deleteById("isbn10:8996991341")
            assertNull(awaitItem())
        }
    }

    @Test
    fun `기존 저장 정가를 보존하면서 가격 필터 적용 여부에 따라 도서를 유지하거나 제외한다`() = runTest {
        val prices = listOf(null, Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE)
        prices.forEachIndexed { index, price ->
            // 기존 DB에 이미 존재하는 값을 API 매핑 없이 직접 넣어 읽기 경로를 검증합니다.
            dao.upsert(book(index.toString()).toFavoriteEntity(savedAt = index.toLong()).copy(price = price))
        }
        val restored = dao.observeAll().first().map { it.toBook() }
        assertEquals(prices.size, restored.size)
        restored.forEach { book ->
            val originalPrice = prices[book.id.toInt()]
            assertEquals(originalPrice, book.price)
            assertEquals(originalPrice?.takeIf { it > 0 }, book.availablePrice)
            assertEquals(originalPrice, book.toFavoriteEntity(savedAt = 0L).price)
        }
        val unfiltered = FavoriteListRules.apply(restored, "", FavoriteSort.TitleAscending, PriceFilter.None)
        assertEquals(prices.indices.map(Int::toString).toSet(), unfiltered.map { it.id }.toSet())
        val filtered = FavoriteListRules.apply(restored, "", FavoriteSort.TitleAscending, PriceFilter(min = 0))
        assertEquals(setOf("4", "5"), filtered.map { it.id }.toSet())
        val zeroOnly = FavoriteListRules.apply(restored, "", FavoriteSort.TitleAscending, PriceFilter(max = 0))
        assertEquals(emptyList<Book>(), zeroOnly)
        assertEquals(prices.size, dao.observeAll().first().size)
    }

    @Test
    fun `같은 ID를 다시 저장하면 한 행만 유지한다`() = runTest {
        dao.upsert(book("a", title = "이전").toFavoriteEntity(savedAt = 1L))
        dao.upsert(book("a", title = "최신").toFavoriteEntity(savedAt = 2L))
        dao.upsert(book("b").toFavoriteEntity(savedAt = 3L))

        dao.observeAll().test {
            val all = awaitItem()
            assertEquals(listOf("b", "a"), all.map { it.id })
            assertEquals("최신", all.last().title)
        }
        dao.observeIds().test {
            assertEquals(setOf("a", "b"), awaitItem().toSet())
        }
    }

    @Test
    fun `없는 ID 삭제는 아무 일도 하지 않는다`() = runTest {
        dao.deleteById("missing")
        dao.observeIds().test { assertEquals(emptyList<String>(), awaitItem()) }
    }

    @Test
    fun `파일 DB를 닫았다 다시 열어도 저장한 도서가 남아 있다`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reopen-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, BookSearchDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()

        val first = open()
        first.favoriteBookDao().upsert(book("a").toFavoriteEntity(savedAt = 1L))
        first.close()

        val reopened = open()
        try {
            assertEquals(book("a"), reopened.favoriteBookDao().observeById("a").first()?.toBook())
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }
}
