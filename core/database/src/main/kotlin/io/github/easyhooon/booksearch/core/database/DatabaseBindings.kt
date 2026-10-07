package io.github.easyhooon.booksearch.core.database

import android.content.Context
import androidx.room.Room
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface DatabaseBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun provideBookSearchDatabase(context: Context): BookSearchDatabase =
        Room.databaseBuilder(context, BookSearchDatabase::class.java, BookSearchDatabase.NAME).build()

    @Provides
    fun provideFavoriteBookDao(database: BookSearchDatabase): FavoriteBookDao = database.favoriteBookDao()
}
