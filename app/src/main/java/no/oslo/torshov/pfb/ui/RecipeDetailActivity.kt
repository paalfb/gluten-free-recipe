package no.oslo.torshov.pfb.ui

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
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
    private var recipeId: Long = -1L

    private val experiencesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val date = result.data?.getStringExtra(ExperiencesActivity.RESULT_OPEN_CALENDAR_DATE)
            if (date != null) {
                val intent = Intent(this, CalendarActivity::class.java)
                intent.putExtra(CalendarActivity.EXTRA_DATE, date)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recipeId = intent.getLongExtra(EXTRA_RECIPE_ID, -1L)
        if (recipeId == -1L) { finish(); return }

        viewModel.loadRecipe(recipeId)
        viewModel.recipeName.observe(this) { binding.toolbar.title = it }
        viewModel.category.observe(this) { binding.categoryChip.text = RecipeCategory.displayName(this, it) }
        viewModel.isFavourite.observe(this) { invalidateOptionsMenu() }
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isFav = viewModel.isFavourite.value ?: false
        menu.findItem(R.id.action_toggle_favourite)?.title = getString(
            if (isFav) R.string.action_remove_favourite else R.string.action_add_favourite
        )
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_share_recipe -> { shareRecipe(); true }
        R.id.action_rename_recipe -> { showRenameDialog(); true }
        R.id.action_toggle_favourite -> { viewModel.toggleFavourite(); true }
        R.id.action_change_emoji -> { showChangeEmojiDialog(); true }
        R.id.action_experiences -> { openExperiences(); true }
        R.id.action_delete_recipe -> { confirmDelete(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openExperiences() {
        val intent = Intent(this, ExperiencesActivity::class.java)
        intent.putExtra(ExperiencesActivity.EXTRA_RECIPE_ID, recipeId)
        intent.putExtra(ExperiencesActivity.EXTRA_RECIPE_NAME, viewModel.recipeName.value ?: "")
        experiencesLauncher.launch(intent)
    }

    private fun showChangeEmojiDialog() {
        val emojis = listOf("🍞","🥐","🥖","🥯","🫓","🧁","🎂","🍰","🍩","🍪","🧇","🥞","🥗","🍕","🫕","🥨","🍫","🌾","🔥")
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(
                (24 * resources.displayMetrics.density).toInt(), 0,
                (24 * resources.displayMetrics.density).toInt(), 0
            )
        }
        val scrollRow = android.widget.HorizontalScrollView(this)
        val row = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.HORIZONTAL }
        val dp44 = (44 * resources.displayMetrics.density).toInt()
        val dp4  = (4  * resources.displayMetrics.density).toInt()
        val dp2  = (2  * resources.displayMetrics.density).toInt()
        val current = viewModel.currentEmoji
        val selectedEmojis = emojis.filter { it in current }.toMutableList()
        val allBtns = mutableListOf<android.widget.TextView>()

        fun makeSelectedBackground(): android.graphics.drawable.GradientDrawable {
            val tv = android.util.TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, tv, true)
            return android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp4.toFloat()
                setStroke(dp2, tv.data)
                setColor(android.graphics.Color.TRANSPARENT)
            }
        }

        fun refreshBackgrounds() {
            allBtns.forEach { btn ->
                btn.background = if (btn.tag in selectedEmojis || (btn.tag == "" && selectedEmojis.isEmpty()))
                    makeSelectedBackground() else null
            }
        }

        fun toggleEmoji(e: String) {
            if (e == "") { selectedEmojis.clear() }
            else if (e in selectedEmojis) { selectedEmojis.remove(e) }
            else if (selectedEmojis.size < 2) { selectedEmojis.add(e) }
            refreshBackgrounds()
        }

        val noneBtn = android.widget.TextView(this).apply {
            text = "—"; textSize = 20f; tag = ""
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(dp44, dp44).also { it.marginEnd = dp4 }
        }
        allBtns.add(noneBtn)
        row.addView(noneBtn)
        noneBtn.setOnClickListener { toggleEmoji("") }

        emojis.forEach { e ->
            val btn = android.widget.TextView(this).apply {
                text = e; textSize = 24f; tag = e
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(dp44, dp44).also { it.marginEnd = dp4 }
            }
            allBtns.add(btn)
            row.addView(btn)
            btn.setOnClickListener { toggleEmoji(e) }
        }
        refreshBackgrounds()
        scrollRow.addView(row)
        container.addView(scrollRow)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_change_emoji)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ -> viewModel.updateEmoji(selectedEmojis.joinToString("")) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCategoryDialog() {
        val categories = RecipeCategory.ALL.toTypedArray()
        val displayNames = categories.map { RecipeCategory.displayName(this, it) }.toTypedArray()
        val currentCategory = viewModel.category.value ?: RecipeCategory.OTHER
        val checkedIndex = categories.indexOf(currentCategory).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.category_title)
            .setSingleChoiceItems(displayNames, checkedIndex) { dialog, which ->
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
