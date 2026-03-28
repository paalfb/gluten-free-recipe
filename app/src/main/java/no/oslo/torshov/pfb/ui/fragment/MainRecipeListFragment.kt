package no.oslo.torshov.pfb.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.model.Recipe
import no.oslo.torshov.pfb.ui.RecipeDetailActivity
import no.oslo.torshov.pfb.ui.adapter.RecipeAdapter
import no.oslo.torshov.pfb.ui.viewmodel.MainViewModel

class MainRecipeListFragment : Fragment() {

    companion object {
        private const val ARG_WITH_THICKENERS = "with_thickeners"

        fun newInstance(withThickeners: Boolean) = MainRecipeListFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_WITH_THICKENERS, withThickeners) }
        }
    }

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: RecipeAdapter
    private lateinit var emptyView: TextView
    private lateinit var chipGroup: ChipGroup
    private var withThickeners = false
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

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val categoryScrollView = view.findViewById<View>(R.id.categoryScrollView)
        emptyView = view.findViewById(R.id.emptyView)
        chipGroup = view.findViewById(R.id.categoryChipGroup)

        emptyView.text = getString(
            if (withThickeners) R.string.empty_with_thickeners else R.string.empty_without_thickeners
        )

        adapter = RecipeAdapter { recipe ->
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val recipe = adapter.currentList[position]
                adapter.notifyItemChanged(position)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_confirm_title)
                    .setMessage(getString(R.string.delete_confirm_message, recipe.name))
                    .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteRecipe(recipe) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }).attachToRecyclerView(recyclerView)

        val liveData = if (withThickeners) viewModel.recipesWithThickeners else viewModel.recipesWithoutThickeners
        liveData.observe(viewLifecycleOwner) { recipes ->
            fullList = recipes
            val categories = recipes.map { it.category }.distinct().sorted()
            if (categories.isNotEmpty()) {
                categoryScrollView.visibility = View.VISIBLE
                rebuildChips(categories)
            } else {
                categoryScrollView.visibility = View.GONE
                selectedCategory = null
            }
            applyFilter()
        }
    }

    private fun rebuildChips(categories: List<String>) {
        // Validate selectedCategory still exists
        if (selectedCategory != null && selectedCategory !in categories) selectedCategory = null

        chipGroup.removeAllViews()
        addChip(getString(R.string.category_all), selected = selectedCategory == null) {
            selectedCategory = null
            updateChipSelection()
            applyFilter()
        }
        categories.forEach { cat ->
            addChip(cat, selected = selectedCategory == cat) {
                selectedCategory = cat
                updateChipSelection()
                applyFilter()
            }
        }
    }

    private fun addChip(label: String, selected: Boolean, onClick: () -> Unit) {
        val chip = Chip(requireContext()).apply {
            text = label
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
                else -> chip.text == selectedCategory
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
