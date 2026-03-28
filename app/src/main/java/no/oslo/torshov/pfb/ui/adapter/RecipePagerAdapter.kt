package no.oslo.torshov.pfb.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import no.oslo.torshov.pfb.ui.fragment.FreetextTabFragment
import no.oslo.torshov.pfb.ui.fragment.RecipeListTabFragment

class RecipePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        2, 3 -> FreetextTabFragment.newInstance(position)
        else -> RecipeListTabFragment.newInstance(position)
    }
}
