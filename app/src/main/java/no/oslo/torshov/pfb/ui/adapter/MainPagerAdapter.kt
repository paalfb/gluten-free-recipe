package no.oslo.torshov.pfb.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import no.oslo.torshov.pfb.ui.fragment.CalendarTabFragment
import no.oslo.torshov.pfb.ui.fragment.MainRecipeListFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> MainRecipeListFragment.newInstance(withThickeners = true)
        1 -> MainRecipeListFragment.newInstance(withThickeners = false)
        else -> CalendarTabFragment()
    }
}
