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

class MainRecipeListFragment : Fragment() {

    companion object {
        private const val ARG_WITH_THICKENERS = "with_thickeners"
        private const val ARG_FAVOURITES_ONLY = "favourites_only"

        fun newInstance(withThickeners: Boolean) = MainRecipeListFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_WITH_THICKENERS, withThickeners) }
        }

        fun newFavourites() = MainRecipeListFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_FAVOURITES_ONLY, true) }
        }
    }

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: RecipeAdapter
    private lateinit var emptyView: TextView
    private lateinit var chipGroup: ChipGroup
    private var withThickeners = false
    private var favouritesOnly = false
    private var fullList: List<Recipe> = emptyList()
    private var selectedCategory: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_recipe_list_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        withThickeners = arguments?.getBoolean(ARG_WITH_THICKENERS) ?: false
        favouritesOnly = arguments?.getBoolean(ARG_FAVOURITES_ONLY) ?: false

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val categoryScrollView = view.findViewById<View>(R.id.categoryScrollView)
        emptyView = view.findViewById(R.id.emptyView)
        chipGroup = view.findViewById(R.id.categoryChipGroup)

        emptyView.text = getString(when {
            favouritesOnly -> R.string.empty_favourites
            withThickeners -> R.string.empty_with_thickeners
            else -> R.string.empty_without_thickeners
        })

        adapter = RecipeAdapter(
            onClick = { recipe ->
                val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.id)
                startActivity(intent)
            },
            onLongClick = { recipe -> viewModel.toggleTested(recipe) },
            onExperiencesClick = { recipe ->
                val intent = Intent(requireContext(), ExperiencesActivity::class.java)
                intent.putExtra(ExperiencesActivity.EXTRA_RECIPE_ID, recipe.id)
                intent.putExtra(ExperiencesActivity.EXTRA_RECIPE_NAME, recipe.name)
                startActivity(intent)
            },
            onFavouriteClick = { recipe -> viewModel.toggleFavourite(recipe) }
        )
        recyclerView.adapter = adapter

        val liveData = when {
            favouritesOnly -> viewModel.recipes
            withThickeners -> viewModel.recipesWithThickeners
            else -> viewModel.recipesWithoutThickeners
        }
        liveData.observe(viewLifecycleOwner) { recipes ->
            fullList = if (favouritesOnly) recipes.filter { it.favourite } else recipes
            val categories = fullList.map { it.category }.filter { it.isNotEmpty() }.distinct()
                .sortedBy { RecipeCategory.displayName(requireContext(), it) }
            if (categories.isNotEmpty()) {
                categoryScrollView.visibility = View.VISIBLE
                rebuildChips(categories)
            } else {
                categoryScrollView.visibility = View.GONE
                selectedCategory = null
            }
            applyFilter()
        }

        viewModel.recipesWithExperiences.observe(viewLifecycleOwner) { ids ->
            adapter.recipesWithExperiences = ids
        }
    }

    private fun rebuildChips(categories: List<String>) {
        // Validate selectedCategory still exists
        if (selectedCategory != null && selectedCategory !in categories) selectedCategory = null

        chipGroup.removeAllViews()
        addChip(getString(R.string.category_all), key = null, selected = selectedCategory == null) {
            selectedCategory = null
            updateChipSelection()
            applyFilter()
        }
        categories.forEach { cat ->
            addChip(RecipeCategory.displayName(requireContext(), cat), key = cat, selected = selectedCategory == cat) {
                selectedCategory = cat
                updateChipSelection()
                applyFilter()
            }
        }
    }

    private fun addChip(label: String, key: String?, selected: Boolean, onClick: () -> Unit) {
        val chip = Chip(requireContext()).apply {
            text = label
            tag = key
            isCheckable = true
            isChecked = selected
            setOnClickListener { onClick() }
        }
        chipGroup.addView(chip)
    }

    private fun updateChipSelection() {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            chip.isChecked = when (i) {
                0 -> selectedCategory == null
                else -> chip.tag == selectedCategory
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (selectedCategory == null) fullList
                       else fullList.filter { it.category == selectedCategory }
        adapter.submitList(filtered)
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
