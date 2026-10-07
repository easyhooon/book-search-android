package io.github.easyhooon.booksearch.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [FavoriteBookEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BookSearchDatabase : RoomDatabase() {
    abstract fun favoriteBookDao(): FavoriteBookDao

    companion object {
        const val NAME = "book-search.db"
    }
}
