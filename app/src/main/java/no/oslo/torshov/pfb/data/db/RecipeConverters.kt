package no.oslo.torshov.pfb.data.db

import androidx.room.TypeConverter

class RecipeConverters {

    @TypeConverter
    fun fromList(list: MutableList<String>): String = list.joinToString("\u001F")

    @TypeConverter
    fun toList(value: String): MutableList<String> =
        if (value.isEmpty()) mutableListOf()
        else value.split("\u001F").toMutableList()
}
