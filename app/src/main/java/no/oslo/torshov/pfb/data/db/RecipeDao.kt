package no.oslo.torshov.pfb.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import no.oslo.torshov.pfb.data.model.Recipe

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY id DESC")
    suspend fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): Recipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: Recipe)

    @Update
    suspend fun update(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<Recipe>)

    @Query("SELECT name FROM recipes")
    suspend fun getAllNames(): List<String>

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(recipe: Recipe)
}
