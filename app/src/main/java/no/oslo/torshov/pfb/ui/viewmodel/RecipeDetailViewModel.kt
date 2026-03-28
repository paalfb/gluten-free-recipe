package no.oslo.torshov.pfb.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import no.oslo.torshov.pfb.data.db.AppDatabase
import no.oslo.torshov.pfb.data.model.Recipe
import no.oslo.torshov.pfb.data.repository.RecipeJsonSerializer
import no.oslo.torshov.pfb.data.repository.RecipeRepository

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository(AppDatabase.getInstance(application).recipeDao())

    private lateinit var recipe: Recipe

    private val _recipeName = MutableLiveData<String>()
    val recipeName: LiveData<String> = _recipeName

    private val _category = MutableLiveData<String>()
    val category: LiveData<String> = _category

    private val _ingredients = MutableLiveData<List<String>>(emptyList())
    private val _steps = MutableLiveData<List<String>>(emptyList())
    private val _tips = MutableLiveData<List<String>>(emptyList())
    private val _commonMistakes = MutableLiveData<List<String>>(emptyList())

    private val _tipsText = MutableLiveData<String>()
    val tipsText: LiveData<String> = _tipsText

    private val _commonMistakesText = MutableLiveData<String>()
    val commonMistakesText: LiveData<String> = _commonMistakesText

    fun loadRecipe(recipeId: Long) {
        if (::recipe.isInitialized) return
        viewModelScope.launch {
            recipe = repository.getById(recipeId) ?: return@launch
            _recipeName.value = recipe.name
            _category.value = recipe.category
            _ingredients.value = recipe.ingredients.toList()
            _steps.value = recipe.steps.toList()
            _tipsText.value = recipe.tips.joinToString("\n")
            _commonMistakesText.value = recipe.commonMistakes.joinToString("\n")
        }
    }

    fun getFreetextLiveData(tabPosition: Int): LiveData<String> = when (tabPosition) {
        2 -> tipsText
        3 -> commonMistakesText
        else -> throw IllegalArgumentException("Not a freetext tab: $tabPosition")
    }

    fun updateFreetextSilent(tabPosition: Int, text: String) {
        if (!::recipe.isInitialized) return
        when (tabPosition) {
            2 -> { recipe.tips.clear(); recipe.tips.add(text) }
            3 -> { recipe.commonMistakes.clear(); recipe.commonMistakes.add(text) }
        }
        viewModelScope.launch { repository.update(recipe) }
    }

    fun getItemsLiveData(tabPosition: Int): LiveData<List<String>> = when (tabPosition) {
        0 -> _ingredients
        1 -> _steps
        2 -> _tips
        3 -> _commonMistakes
        else -> throw IllegalArgumentException("Invalid tab: $tabPosition")
    }

    fun addItem(tabPosition: Int, item: String) {
        if (!::recipe.isInitialized) return
        when (tabPosition) {
            0 -> { recipe.ingredients.add(item); _ingredients.value = recipe.ingredients.toList() }
            1 -> { recipe.steps.add(item); _steps.value = recipe.steps.toList() }
            2 -> { recipe.tips.add(item); _tips.value = recipe.tips.toList() }
            3 -> { recipe.commonMistakes.add(item); _commonMistakes.value = recipe.commonMistakes.toList() }
        }
        viewModelScope.launch { repository.update(recipe) }
    }

    fun updateItem(tabPosition: Int, position: Int, newText: String) {
        if (!::recipe.isInitialized) return
        when (tabPosition) {
            0 -> { recipe.ingredients[position] = newText; _ingredients.value = recipe.ingredients.toList() }
            1 -> { recipe.steps[position] = newText; _steps.value = recipe.steps.toList() }
            2 -> { recipe.tips[position] = newText; _tips.value = recipe.tips.toList() }
            3 -> { recipe.commonMistakes[position] = newText; _commonMistakes.value = recipe.commonMistakes.toList() }
        }
        viewModelScope.launch { repository.update(recipe) }
    }

    // Updates in-memory and persists to DB without emitting LiveData,
    // so the EditText isn't rebound while the user is actively typing.
    fun updateItemSilent(tabPosition: Int, position: Int, newText: String) {
        if (!::recipe.isInitialized) return
        when (tabPosition) {
            0 -> recipe.ingredients[position] = newText
            1 -> recipe.steps[position] = newText
            2 -> recipe.tips[position] = newText
            3 -> recipe.commonMistakes[position] = newText
        }
        viewModelScope.launch { repository.update(recipe) }
    }

    fun updateTipsTextSilent(text: String) = updateFreetextSilent(2, text)

    fun insertItemsAt(tabPosition: Int, position: Int, items: List<String>) {
        if (!::recipe.isInitialized) return
        val list = when (tabPosition) {
            0 -> recipe.ingredients
            1 -> recipe.steps
            2 -> recipe.tips
            3 -> recipe.commonMistakes
            else -> return
        }
        list[position] = items[0]
        list.addAll(position + 1, items.drop(1))
        when (tabPosition) {
            0 -> _ingredients.value = recipe.ingredients.toList()
            1 -> _steps.value = recipe.steps.toList()
            2 -> _tips.value = recipe.tips.toList()
            3 -> _commonMistakes.value = recipe.commonMistakes.toList()
        }
        viewModelScope.launch { repository.update(recipe) }
    }

    fun setItems(tabPosition: Int, newItems: List<String>) {
        if (!::recipe.isInitialized) return
        val list = when (tabPosition) {
            0 -> recipe.ingredients
            1 -> recipe.steps
            2 -> recipe.tips
            3 -> recipe.commonMistakes
            else -> return
        }
        list.clear()
        list.addAll(newItems)
        viewModelScope.launch { repository.update(recipe) }
    }

    fun removeItem(tabPosition: Int, position: Int) {
        if (!::recipe.isInitialized) return
        when (tabPosition) {
            0 -> { recipe.ingredients.removeAt(position); _ingredients.value = recipe.ingredients.toList() }
            1 -> { recipe.steps.removeAt(position); _steps.value = recipe.steps.toList() }
            2 -> { recipe.tips.removeAt(position); _tips.value = recipe.tips.toList() }
            3 -> { recipe.commonMistakes.removeAt(position); _commonMistakes.value = recipe.commonMistakes.toList() }
        }
        viewModelScope.launch { repository.update(recipe) }
    }

    fun exportRecipe(onReady: (String) -> Unit) {
        if (!::recipe.isInitialized) return
        viewModelScope.launch {
            onReady(RecipeJsonSerializer.toJson(listOf(recipe)))
        }
    }

    fun deleteRecipe(onDeleted: () -> Unit) {
        if (!::recipe.isInitialized) return
        viewModelScope.launch {
            repository.delete(recipe)
            onDeleted()
        }
    }

    fun renameRecipe(newName: String) {
        if (!::recipe.isInitialized) return
        recipe = recipe.copy(name = newName)
        _recipeName.value = newName
        viewModelScope.launch { repository.update(recipe) }
    }

    fun updateCategory(newCategory: String) {
        if (!::recipe.isInitialized) return
        recipe = recipe.copy(category = newCategory)
        _category.value = newCategory
        viewModelScope.launch { repository.update(recipe) }
    }
}
