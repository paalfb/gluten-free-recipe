package no.oslo.torshov.pfb.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.db.AppDatabase
import no.oslo.torshov.pfb.data.model.Recipe
import no.oslo.torshov.pfb.data.repository.RecipeJsonSerializer
import no.oslo.torshov.pfb.data.repository.RecipeRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository(AppDatabase.getInstance(application).recipeDao())
    private val experienceDao = AppDatabase.getInstance(application).recipeExperienceDao()

    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    val pendingCalendarDate = MutableLiveData<String?>(null)

    private val _recipesWithExperiences = MutableLiveData<Set<Long>>(emptySet())
    val recipesWithExperiences: LiveData<Set<Long>> = _recipesWithExperiences

    private val thickenerRegex = Regex("[Ee]4\\d\\d")

    val recipesWithThickeners: LiveData<List<Recipe>> = _recipes.map { list ->
        list.filter { recipe -> recipe.ingredients.any { thickenerRegex.containsMatchIn(it) } }
    }

    val recipesWithoutThickeners: LiveData<List<Recipe>> = _recipes.map { list ->
        list.filter { recipe -> recipe.ingredients.none { thickenerRegex.containsMatchIn(it) } }
    }

    fun loadBundledRecipes() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val json = app.resources.openRawResource(R.raw.bundled_recipes)
                .bufferedReader().readText()
            val bundled = RecipeJsonSerializer.fromJson(json)
            val existing = repository.getAll()
            val newRecipes = bundled.filter { candidate ->
                existing.none { it.name == candidate.name &&
                    it.ingredients.sorted() == candidate.ingredients.sorted() }
            }
            if (newRecipes.isNotEmpty()) {
                repository.importAll(newRecipes)
                _recipes.value = repository.getAll()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _recipes.value = repository.getAll()
            _recipesWithExperiences.value = experienceDao.getRecipeIdsWithExperiences().toSet()
        }
    }

    fun addRecipe(name: String, emoji: String = "") {
        viewModelScope.launch {
            repository.add(Recipe(name = name, emoji = emoji))
            _recipes.value = repository.getAll()
        }
    }

    fun exportRecipesAsPdf(onReady: (List<Recipe>) -> Unit) {
        viewModelScope.launch {
            onReady(repository.getAll())
        }
    }

    fun exportRecipes(onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(RecipeJsonSerializer.toJson(repository.getAll()))
        }
    }

    fun exportRecipesFiltered(filter: (Recipe) -> Boolean, onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(RecipeJsonSerializer.toJson(repository.getAll().filter(filter)))
        }
    }

    fun exportSync(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val recipes = repository.getAll()
            val expMap = recipes.associate { recipe ->
                recipe.id to experienceDao.getForRecipe(recipe.id)
            }
            onReady(RecipeJsonSerializer.toSyncJson(recipes, expMap))
        }
    }

    fun importSync(json: String, onComplete: (recipes: Int, experiences: Int) -> Unit) {
        viewModelScope.launch {
            val data = RecipeJsonSerializer.fromSyncJson(json)
            val existing = repository.getAll()
            var newRecipeCount = 0
            val nameToId = mutableMapOf<String, Long>()
            existing.forEach { nameToId[it.name] = it.id }

            data.recipes.forEach { candidate ->
                val match = existing.find { it.name == candidate.name &&
                    it.ingredients.sorted() == candidate.ingredients.sorted() }
                if (match == null) {
                    val id = repository.add(candidate)
                    nameToId[candidate.name] = id
                    newRecipeCount++
                } else {
                    nameToId[candidate.name] = match.id
                    if (match.favourite != candidate.favourite) {
                        repository.update(match.copy(
                            favourite = match.favourite || candidate.favourite
                        ))
                    }
                }
            }

            var newExpCount = 0
            data.experiencesByRecipeName.forEach { (recipeName, exps) ->
                val recipeId = nameToId[recipeName] ?: return@forEach
                val existing = experienceDao.getForRecipe(recipeId)
                exps.forEach { (date, note) ->
                    if (existing.none { it.date == date && it.note == note }) {
                        experienceDao.insert(no.oslo.torshov.pfb.data.model.RecipeExperience(
                            recipeId = recipeId, date = date, note = note))
                        newExpCount++
                    }
                }
            }

            _recipes.value = repository.getAll()
            _recipesWithExperiences.value = experienceDao.getRecipeIdsWithExperiences().toSet()
            onComplete(newRecipeCount, newExpCount)
        }
    }

    fun importRecipes(json: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val candidates = RecipeJsonSerializer.fromJson(json)
            val existing = repository.getAll()
            val newRecipes = candidates.filter { candidate ->
                existing.none { it.name == candidate.name &&
                    it.ingredients.sorted() == candidate.ingredients.sorted() }
            }
            if (newRecipes.isNotEmpty()) repository.importAll(newRecipes)
            _recipes.value = repository.getAll()
            onComplete(newRecipes.size)
        }
    }

    fun toggleFavourite(recipe: Recipe) {
        viewModelScope.launch {
            repository.update(recipe.copy(favourite = !recipe.favourite))
            _recipes.value = repository.getAll()
        }
    }

    fun deleteAllRecipes() {
        viewModelScope.launch {
            repository.deleteAll()
            _recipes.value = emptyList()
        }
    }
}
