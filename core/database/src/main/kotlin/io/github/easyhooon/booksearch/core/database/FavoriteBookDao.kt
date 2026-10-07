package io.github.easyhooon.booksearch.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteBookDao {

    /** 정렬은 화면 규칙(Collator)으로 메모리에서 하므로 저장 순서만 고정합니다. */
    @Query("SELECT * FROM favorite_books ORDER BY saved_at DESC, id ASC")
    fun observeAll(): Flow<List<FavoriteBookEntity>>

    @Query("SELECT id FROM favorite_books")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT * FROM favorite_books WHERE id = :id")
    fun observeById(id: String): Flow<FavoriteBookEntity?>

    @Upsert
    suspend fun upsert(entity: FavoriteBookEntity)

    @Query("DELETE FROM favorite_books WHERE id = :id")
    suspend fun deleteById(id: String)
}
