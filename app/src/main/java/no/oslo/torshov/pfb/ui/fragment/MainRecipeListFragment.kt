package no.oslo.torshov.pfb.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.model.Recipe
import no.oslo.torshov.pfb.data.model.RecipeCategory
import no.oslo.torshov.pfb.ui.ExperiencesActivity
import no.oslo.torshov.pfb.ui.RecipeDetailActivity
import no.oslo.torshov.pfb.ui.adapter.RecipeAdapter
import no.oslo.torshov.pfb.ui.viewmodel.MainViewModel
import no.oslo.torshov.pfb.ui.viewmodel.StabiliserFilter

class MainRecipeListFragment : Fragment() {

    companion object {
        private const val ARG_FAVOURITES_ONLY = "favourites_only"

        fun newInstance() = MainRecipeListFragment()

        fun newFavourites() = MainRecipeListFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_FAVOURITES_ONLY, true) }
        }
    }

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: RecipeAdapter
    private lateinit var emptyView: TextView
    private lateinit var categoryChipGroup: ChipGroup

    private var favouritesOnly = false
    private var localCategoryFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_recipe_list_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup)

        adapter = RecipeAdapter(
            onClick = { recipe ->
                startActivity(Intent(requireContext(), RecipeDetailActivity::class.java).apply {
                    putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.id)
                })
            },
            onExperiencesClick = { recipe ->
                startActivity(Intent(requireContext(), ExperiencesActivity::class.java).apply {
                    putExtra(ExperiencesActivity.EXTRA_RECIPE_ID, recipe.id)
                    putExtra(ExperiencesActivity.EXTRA_RECIPE_NAME, recipe.name)
                })
            }
        )
        recyclerView.adapter = adapter

        favouritesOnly = arguments?.getBoolean(ARG_FAVOURITES_ONLY, false) ?: false

        if (!favouritesOnly) {
            viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
                val categories = recipes
                    .map { it.category }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sortedBy { RecipeCategory.displayName(requireContext(), it) }
                if (categories.isNotEmpty()) {
                    categoryChipGroup.visibility = View.VISIBLE
                    rebuildCategoryChips(categories, emptyList())
                } else {
                    categoryChipGroup.visibility = View.GONE
                    viewModel.categoryFilter.value = null
                }
            }

            viewModel.filteredRecipes.observe(viewLifecycleOwner) { recipes ->
                adapter.submitList(recipes)
                val noFiltersActive = viewModel.stabiliserFilter.value == StabiliserFilter.ALL
                    && viewModel.categoryFilter.value == null
                emptyView.text = getString(
                    if (noFiltersActive) R.string.empty_recipes else R.string.empty_filtered
                )
                emptyView.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
            }
        } else {
            viewModel.filteredFavourites.observe(viewLifecycleOwner) { favourites ->
                val categories = favourites
                    .map { it.category }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sortedBy { RecipeCategory.displayName(requireContext(), it) }
                if (localCategoryFilter != null && localCategoryFilter !in categories) {
                    localCategoryFilter = null
                }
                if (categories.isNotEmpty()) {
                    categoryChipGroup.visibility = View.VISIBLE
                    rebuildCategoryChips(categories, favourites)
                } else {
                    categoryChipGroup.visibility = View.GONE
                }
                applyFavoritesFilter(favourites)
            }
        }

        viewModel.recipesWithExperiences.observe(viewLifecycleOwner) { ids ->
            adapter.recipesWithExperiences = ids
        }
    }

    private fun rebuildCategoryChips(categories: List<String>, fullList: List<Recipe>) {
        if (!favouritesOnly) {
            val current = viewModel.categoryFilter.value
            if (current != null && current !in categories) viewModel.categoryFilter.value = null
        }
        categoryChipGroup.removeAllViews()
        addCategoryChip(getString(R.string.category_all), key = null, fullList = fullList)
        categories.forEach { cat ->
            addCategoryChip(RecipeCategory.displayName(requireContext(), cat), key = cat, fullList = fullList)
        }
    }

    private fun addCategoryChip(label: String, key: String?, fullList: List<Recipe>) {
        val chip = (layoutInflater.inflate(R.layout.item_chip_filter, categoryChipGroup, false) as Chip).apply {
            text = label
            tag = key
            isChecked = if (favouritesOnly) key == localCategoryFilter else key == viewModel.categoryFilter.value
            setOnClickListener {
                if (favouritesOnly) {
                    localCategoryFilter = key
                    applyFavoritesFilter(fullList)
                    syncCategoryChips(key)
                } else {
                    viewModel.categoryFilter.value = key
                    syncCategoryChips(key)
                }
            }
        }
        categoryChipGroup.addView(chip)
    }

    private fun syncCategoryChips(selected: String?) {
        for (i in 0 until categoryChipGroup.childCount) {
            (categoryChipGroup.getChildAt(i) as? Chip)?.isChecked =
                categoryChipGroup.getChildAt(i).tag == selected
        }
    }

    private fun applyFavoritesFilter(fullList: List<Recipe>) {
        val filtered = if (localCategoryFilter == null) fullList
        else fullList.filter { it.category == localCategoryFilter }
        adapter.submitList(filtered)
        emptyView.text = getString(R.string.empty_favourites)
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
