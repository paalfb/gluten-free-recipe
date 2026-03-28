package no.oslo.torshov.pfb.ui

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.model.RecipeCategory
import no.oslo.torshov.pfb.databinding.ActivityRecipeDetailBinding
import no.oslo.torshov.pfb.ui.adapter.RecipePagerAdapter
import no.oslo.torshov.pfb.ui.viewmodel.RecipeDetailViewModel
import java.io.File
import kotlin.math.abs

class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id"
    }

    private lateinit var binding: ActivityRecipeDetailBinding
    private val viewModel: RecipeDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recipeId = intent.getLongExtra(EXTRA_RECIPE_ID, -1L)
        if (recipeId == -1L) { finish(); return }

        viewModel.loadRecipe(recipeId)
        viewModel.recipeName.observe(this) { binding.toolbar.title = it }
        viewModel.category.observe(this) { binding.categoryChip.text = it }
        binding.categoryChip.setOnClickListener { showCategoryDialog() }

        binding.viewPager.adapter = RecipePagerAdapter(this)

        val tabIcons = listOf(
            R.drawable.ic_tab_ingredients,
            R.drawable.ic_tab_steps,
            R.drawable.ic_tab_tips,
            R.drawable.ic_tab_common_mistakes
        )
        val tabDescriptions = listOf(
            R.string.tab_ingredients,
            R.string.tab_steps,
            R.string.tab_tips,
            R.string.tab_common_mistakes
        )

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.icon = ContextCompat.getDrawable(this, tabIcons[position])
            tab.contentDescription = getString(tabDescriptions[position])
        }.attach()

        setupSwipeBackOnFirstTab()
    }

    private fun setupSwipeBackOnFirstTab() {
        var currentPage = 0
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { currentPage = position }
        })

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                if (currentPage == 0 && velocityX > 0 && abs(velocityX) > abs(velocityY)) {
                    onBackPressedDispatcher.onBackPressed()
                    return true
                }
                return false
            }
        })

        (binding.viewPager.getChildAt(0) as? RecyclerView)
            ?.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    detector.onTouchEvent(e)
                    return false
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_share_recipe -> { shareRecipe(); true }
        R.id.action_rename_recipe -> { showRenameDialog(); true }
        R.id.action_delete_recipe -> { confirmDelete(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showCategoryDialog() {
        val categories = RecipeCategory.ALL.toTypedArray()
        val currentCategory = viewModel.category.value ?: RecipeCategory.OTHER
        val checkedIndex = categories.indexOf(currentCategory).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.category_title)
            .setSingleChoiceItems(categories, checkedIndex) { dialog, which ->
                viewModel.updateCategory(categories[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRenameDialog() {
        val currentName = viewModel.recipeName.value ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_recipe, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.editRecipeName)
        nameInput.setText(currentName)
        nameInput.selectAll()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rename_recipe)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = nameInput.text?.toString()?.trim()
                if (!newName.isNullOrEmpty()) viewModel.renameRecipe(newName)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete() {
        val name = viewModel.recipeName.value ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(getString(R.string.delete_confirm_message, name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteRecipe { finish() }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun shareRecipe() {
        viewModel.exportRecipe { json ->
            val safeName = (viewModel.recipeName.value ?: "recipe")
                .replace(Regex("[^\\w\\s-]"), "").trim().replace(' ', '_')
            val file = File(cacheDir, "$safeName.json")
            file.writeText(json)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, viewModel.recipeName.value)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_recipe)))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
