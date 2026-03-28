package no.oslo.torshov.pfb.data.repository

import no.oslo.torshov.pfb.data.model.Recipe
import no.oslo.torshov.pfb.data.model.RecipeCategory
import org.json.JSONArray
import org.json.JSONObject

object RecipeJsonSerializer {

    private const val VERSION = 1

    fun toJson(recipes: List<Recipe>): String {
        val root = JSONObject()
        root.put("version", VERSION)
        val array = JSONArray()
        for (recipe in recipes) {
            array.put(JSONObject().apply {
                put("name", recipe.name)
                put("category", recipe.category)
                put("ingredients", JSONArray(recipe.ingredients))
                put("steps", JSONArray(recipe.steps))
                put("tips", JSONArray(recipe.tips))
                put("commonMistakes", JSONArray(recipe.commonMistakes))
            })
        }
        root.put("recipes", array)
        return root.toString(2)
    }

    fun fromJson(json: String): List<Recipe> {
        val array = JSONObject(json).getJSONArray("recipes")
        return List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            Recipe(
                name = obj.getString("name"),
                category = obj.optString("category", RecipeCategory.OTHER),
                ingredients = obj.getJSONArray("ingredients").toMutableStringList(),
                steps = obj.getJSONArray("steps").toMutableStringList(),
                tips = obj.getJSONArray("tips").toMutableStringList(),
                commonMistakes = obj.getJSONArray("commonMistakes").toMutableStringList()
            )
        }
    }

    private fun JSONArray.toMutableStringList(): MutableList<String> =
        MutableList(length()) { getString(it) }
}
