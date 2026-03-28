package no.oslo.torshov.pfb.data.repository

import no.oslo.torshov.pfb.data.db.RecipeDao
import no.oslo.torshov.pfb.data.model.Recipe

class RecipeRepository(private val dao: RecipeDao) {

    suspend fun getAll(): List<Recipe> = dao.getAll()

    suspend fun getById(id: Long): Recipe? = dao.getById(id)

    suspend fun add(recipe: Recipe) = dao.insert(recipe)

    suspend fun update(recipe: Recipe) = dao.update(recipe)

    suspend fun importAll(recipes: List<Recipe>) = dao.insertAll(recipes)

    suspend fun getAllNames(): List<String> = dao.getAllNames()

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun delete(recipe: Recipe) = dao.delete(recipe)
}
