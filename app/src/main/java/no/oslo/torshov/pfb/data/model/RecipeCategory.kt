package no.oslo.torshov.pfb.data.model

import android.content.Context
import no.oslo.torshov.pfb.R

object RecipeCategory {
    const val BREAD     = "bread"
    const val FLATBREAD = "flatbread"
    const val CAKES     = "cakes"
    const val COOKIES   = "cookies"
    const val BUNS      = "buns"
    const val ROLLS     = "rolls"
    const val SCONES    = "scones"
    const val MUFFINS   = "muffins"
    const val WAFFLES   = "waffles"
    const val PANCAKES  = "pancakes"
    const val PIZZA     = "pizza"
    const val OTHER     = "other"

    val ALL = listOf(BREAD, FLATBREAD, CAKES, COOKIES, BUNS, ROLLS, SCONES, MUFFINS, WAFFLES, PANCAKES, PIZZA, OTHER)

    fun displayName(context: Context, key: String): String = when (key) {
        BREAD     -> context.getString(R.string.category_bread)
        FLATBREAD -> context.getString(R.string.category_flatbread)
        CAKES     -> context.getString(R.string.category_cakes)
        COOKIES   -> context.getString(R.string.category_cookies)
        BUNS      -> context.getString(R.string.category_buns)
        ROLLS     -> context.getString(R.string.category_rolls)
        SCONES    -> context.getString(R.string.category_scones)
        MUFFINS   -> context.getString(R.string.category_muffins)
        WAFFLES   -> context.getString(R.string.category_waffles)
        PANCAKES  -> context.getString(R.string.category_pancakes)
        PIZZA     -> context.getString(R.string.category_pizza)
        OTHER     -> context.getString(R.string.category_other)
        // Legacy Norwegian values (pre-v6 migration fallback)
        "Brød"       -> context.getString(R.string.category_bread)
        "Flatbrød"   -> context.getString(R.string.category_flatbread)
        "Kaker"      -> context.getString(R.string.category_cakes)
        "Kjeks"      -> context.getString(R.string.category_cookies)
        "Boller"     -> context.getString(R.string.category_buns)
        "Rundstykker"-> context.getString(R.string.category_rolls)
        "Scones"     -> context.getString(R.string.category_scones)
        "Muffins"    -> context.getString(R.string.category_muffins)
        "Vafler"     -> context.getString(R.string.category_waffles)
        "Pannekaker" -> context.getString(R.string.category_pancakes)
        "Annet"      -> context.getString(R.string.category_other)
        else         -> key
    }
}
