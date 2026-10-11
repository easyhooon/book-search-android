package io.github.easyhooon.booksearch.core.ui

import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.easyhooon.booksearch.core.data.DefaultFavoriteMutationKeyFactory
import io.github.easyhooon.booksearch.core.database.FavoriteBookDao
import io.github.easyhooon.booksearch.core.database.FavoriteBookEntity
import io.github.easyhooon.booksearch.core.model.Book
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import soil.query.SwrCachePlus
import soil.query.annotation.ExperimentalSoilQueryApi
import soil.query.annotation.InternalSoilQueryApi
import soil.query.compose.LocalMutationClient
import timber.log.Timber
import java.io.IOException
import java.time.Duration

/** 실제 retain·Soil Mutation·DAO 사이에서 화면 종료 전후의 명령 수명을 검증합니다. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalSoilQueryApi::class, InternalSoilQueryApi::class)
class FavoriteMutationLifecycleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class DelayedDao : FavoriteBookDao {
        val rows = MutableStateFlow<Map<String, FavoriteBookEntity>>(emptyMap())
        val started = mutableListOf<String>()
        val writes = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val failingIds = mutableSetOf<String>()
        var delayDeletes = true

        override fun observeAll(): Flow<List<FavoriteBookEntity>> = rows.map { it.values.toList() }
        override fun observeIds(): Flow<List<String>> = rows.map { it.keys.toList() }
        override fun observeById(id: String): Flow<FavoriteBookEntity?> = rows.map { it[id] }

        override suspend fun upsert(entity: FavoriteBookEntity) {
            started += "save:${entity.id}"
            gate.await()
            if (entity.id in failingIds) throw IOException("disk full")
            writes += "save:${entity.id}"
            rows.value = rows.value + (entity.id to entity)
        }

        override suspend fun deleteById(id: String) {
            started += "delete:$id"
            if (delayDeletes) gate.await()
            if (id in failingIds) throw IOException("disk full")
            writes += "delete:$id"
            rows.value = rows.value - id
        }
    }

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = SwrCachePlus(clientScope)
    private val dao = DelayedDao()
    private val factory = DefaultFavoriteMutationKeyFactory(dao)
    private var entry by mutableStateOf<String?>(null)
    private lateinit var registry: RetainedValuesStoreRegistry
    private lateinit var latest: FavoriteToggler

    @After
    fun tearDown() {
        dao.gate.complete(Unit)
        settle()
        clientScope.cancel()
    }

    private fun setContent() {
        composeRule.setContent {
            registry = retainRetainedValuesStoreRegistry()
            CompositionLocalProvider(LocalMutationClient provides client) {
                entry?.let { id ->
                    registry.LocalRetainedValuesStoreProvider(id) {
                        latest = retainFavoriteToggler(factory.create())
                    }
                }
            }
        }
    }

    private fun show(id: String) {
        composeRule.runOnIdle {
            entry = id
        }
        composeRule.waitForIdle()
    }

    private fun retire(): FavoriteToggler {
        val retired = latest
        val id = checkNotNull(entry)
        composeRule.runOnIdle { entry = null }
        composeRule.waitForIdle()
        composeRule.runOnIdle { registry.clearChild(id) }
        composeRule.waitForIdle()
        return retired
    }

    private fun submit(id: String, favorite: Boolean) {
        composeRule.runOnIdle { latest.setFavorite(book(id), favorite) }
        settle()
    }

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
    }

    @Test
    fun `이전 상세 저장을 기다리다 둘 다 닫아도 접수한 요청을 각각 완료한다`() {
        setContent()
        show("detail-a")
        submit("a", true)
        assertEquals(listOf("save:a"), dao.started)
        retire()
        show("detail-b")
        submit("b", true)
        assertEquals(setOf("b"), latest.pendingIds)
        retire()

        // 마지막 UI가 사라져도 쓰기 완료 전 Mutation의 keepAliveTime을 넘길 수 있어야 합니다.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        dao.gate.complete(Unit)
        settle()

        assertEquals(listOf("save:a", "save:b"), dao.writes)
        assertEquals(setOf("a", "b"), dao.rows.value.keys)
        assertTrue(client.mutationStoreView.values.none { it.hasAttachedInstances() })
    }

    @Test
    fun `서로 다른 화면의 같은 도서 변경도 접수 순서를 유지한다`() {
        setContent()
        show("search")
        submit("a", true)
        retire()
        show("detail")
        submit("a", false)
        retire()
        show("favorites")
        submit("a", true)

        dao.gate.complete(Unit)
        settle()

        assertEquals(listOf("save:a", "delete:a", "save:a"), dao.writes)
        assertEquals(setOf("a"), dao.rows.value.keys)
    }

    @Test
    fun `닫힌 화면은 새 요청을 받지 않고 같은 화면의 연타는 한 번만 저장한다`() {
        setContent()
        show("detail-a")
        submit("a", true)
        submit("a", false)
        val retired = retire()
        composeRule.runOnIdle { retired.setFavorite(book("b"), true) }
        dao.gate.complete(Unit)
        settle()

        assertEquals(listOf("save:a"), dao.writes)
        assertEquals(emptySet<String>(), retired.pendingIds)
        assertFalse(retired.hasFailure)
    }

    @Test
    fun `저장 실패는 현재 화면에 알리고 다음 도서 요청은 계속 처리한다`() {
        dao.failingIds += "a"
        setContent()
        show("detail")
        submit("a", true)
        submit("b", true)
        dao.gate.complete(Unit)
        settle()

        assertTrue(latest.hasFailure)
        assertEquals(emptySet<String>(), latest.pendingIds)
        assertEquals(listOf("save:b"), dao.writes)
        composeRule.runOnIdle { latest.consumeFailure() }
        assertFalse(latest.hasFailure)
    }

    @Test
    fun `닫힌 화면의 실패는 기록하되 새 화면의 오류로 전달하지 않는다`() {
        val errors = mutableListOf<Throwable>()
        val tree = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                t?.let(errors::add)
            }
        }
        Timber.plant(tree)
        try {
            dao.failingIds += "a"
            setContent()
            show("detail-a")
            submit("a", true)
            val retired = retire()
            show("detail-b")
            submit("b", true)
            dao.gate.complete(Unit)
            settle()

            assertTrue(errors.single() is IOException)
            assertFalse(retired.hasFailure)
            assertFalse(latest.hasFailure)
            assertEquals(emptySet<String>(), retired.pendingIds)
            assertEquals(emptySet<String>(), latest.pendingIds)
            assertEquals(listOf("save:b"), dao.writes)
        } finally {
            Timber.uproot(tree)
        }
    }

    @Test
    fun `접수와 pop이 같은 프레임이어도 대기 명령을 완료한다`() {
        setContent()
        show("detail")
        val retired = latest
        composeRule.runOnIdle {
            retired.setFavorite(book("a"), true)
            retired.setFavorite(book("b"), true)
            entry = null
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { registry.clearChild("detail") }
        settle()
        assertEquals(listOf("save:a"), dao.started)
        assertEquals(setOf("a", "b"), retired.pendingIds)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        dao.gate.complete(Unit)
        settle()

        assertEquals(listOf("save:a", "save:b"), dao.writes)
        assertEquals(emptySet<String>(), retired.pendingIds)
        assertTrue(client.mutationStoreView.values.none { it.hasAttachedInstances() })
    }

    @Test
    fun `컴포지션 이탈과 복귀에도 같은 객체와 대기 명령을 유지한다`() {
        setContent()
        show("search")
        submit("a", true)
        val retained = latest
        composeRule.runOnIdle { entry = null }
        composeRule.waitForIdle()
        show("search")
        assertSame(retained, latest)
        assertEquals(setOf("a"), latest.pendingIds)
        dao.gate.complete(Unit)
        settle()
        submit("a", false)
        assertEquals(listOf("save:a", "delete:a"), dao.writes)
        assertEquals(emptySet<String>(), dao.rows.value.keys)
    }

    @Test
    fun `느린 저장 뒤 다른 화면의 빠른 해제도 최종 미저장 상태를 유지한다`() {
        dao.delayDeletes = false
        setContent()
        show("search")
        submit("a", true)
        retire()
        show("detail")
        submit("a", false)
        retire()

        dao.gate.complete(Unit)
        settle()

        // DAO 작업 시간에 관계없이 마지막 사용자 의도인 해제가 최종 상태여야 합니다.
        assertEquals(emptySet<String>(), dao.rows.value.keys)
        assertEquals(listOf("save:a", "delete:a"), dao.writes)
    }

    private fun book(id: String) = Book(
        id = id, isbn10 = null, isbn13 = null, title = id, authors = emptyList(), translators = emptyList(),
        publisher = "", publishedDate = null, price = null, salePrice = null, thumbnailUrl = "",
        contents = "", url = "", status = "",
    )
}
