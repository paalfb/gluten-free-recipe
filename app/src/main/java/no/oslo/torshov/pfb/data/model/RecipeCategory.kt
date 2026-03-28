package no.oslo.torshov.pfb.data.model

object RecipeCategory {
    const val BREAD     = "Brød"
    const val FLATBREAD = "Flatbrød"
    const val CAKES     = "Kaker"
    const val COOKIES   = "Kjeks"
    const val BUNS      = "Boller"
    const val ROLLS     = "Rundstykker"
    const val SCONES    = "Scones"
    const val MUFFINS   = "Muffins"
    const val WAFFLES   = "Vaffler"
    const val PANCAKES  = "Pannekaker"
    const val OTHER     = "Annet"

    val ALL = listOf(BREAD, FLATBREAD, CAKES, COOKIES, BUNS, ROLLS, SCONES, MUFFINS, WAFFLES, PANCAKES, OTHER)
}
