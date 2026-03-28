package no.oslo.torshov.pfb.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = RecipeCategory.OTHER,
    val ingredients: MutableList<String> = mutableListOf(),
    val steps: MutableList<String> = mutableListOf(),
    val tips: MutableList<String> = mutableListOf(),
    val commonMistakes: MutableList<String> = mutableListOf()
) : Serializable
