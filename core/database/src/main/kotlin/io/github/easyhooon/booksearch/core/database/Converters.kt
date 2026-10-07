package io.github.easyhooon.booksearch.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal class Converters {
    private val serializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(serializer, value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(serializer, value)
}
