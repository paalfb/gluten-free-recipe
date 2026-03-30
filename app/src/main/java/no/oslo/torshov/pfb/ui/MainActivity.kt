package no.oslo.torshov.pfb.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.databinding.ActivityMainBinding
import no.oslo.torshov.pfb.ui.adapter.MainPagerAdapter
import no.oslo.torshov.pfb.ui.viewmodel.MainViewModel
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_CALENDAR_DATE = "open_calendar_date"
        private const val CALENDAR_TAB_INDEX = 2
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFromUri(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            if (position == 2) {
                val tv = android.widget.TextView(this).apply {
                    text = "⭐"
                    textSize = 16f
                    setPadding(0, 0, 0, 0)
                    gravity = android.view.Gravity.CENTER
                }
                tab.setCustomView(tv)
            } else {
                tab.text = when (position) {
                    0 -> getString(R.string.tab_with_thickeners)
                    else -> getString(R.string.tab_without_thickeners)
                }
            }
        }.attach()

        val starTabDp = (48 * resources.displayMetrics.density).toInt()
        binding.tabLayout.post {
            val tabStrip = binding.tabLayout.getChildAt(0) as? android.widget.LinearLayout ?: return@post
            val totalWidth = binding.tabLayout.width
            val textTabWidth = (totalWidth - starTabDp) / 2
            for (i in 0 until tabStrip.childCount) {
                tabStrip.getChildAt(i).layoutParams =
                    tabStrip.getChildAt(i).layoutParams.apply {
                        width = if (i == 2) starTabDp else textTabWidth
                    }
            }
            tabStrip.requestLayout()
        }

        viewModel.loadBundledRecipes()
        binding.fab.setOnClickListener { showAddRecipeDialog() }

        if (savedInstanceState == null) handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val date = intent.getStringExtra(EXTRA_OPEN_CALENDAR_DATE)
        if (date != null) {
            openCalendar(date)
            return
        }
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_export -> { exportRecipesAsJson(); true }
        R.id.action_export_pdf -> { exportRecipesAsPdf(); true }
        R.id.action_import -> {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            true
        }
        R.id.action_delete_all -> { confirmDeleteAll(); true }
        R.id.action_calendar -> { openCalendar(null); true }
        R.id.action_about -> { showAbout(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openCalendar(date: String?) {
        val intent = Intent(this, CalendarActivity::class.java)
        if (date != null) intent.putExtra(CalendarActivity.EXTRA_DATE, date)
        startActivity(intent)
    }

    private fun showAbout() {
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_about)
            .setMessage(getString(R.string.about_message, version))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun confirmDeleteAll() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_all_confirm_title)
            .setMessage(R.string.delete_all_confirm_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteAllRecipes() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportRecipesAsPdf() {
        viewModel.exportRecipesAsPdf { recipes ->
            val file = no.oslo.torshov.pfb.data.repository.RecipePdfExporter.export(this, recipes)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_subject))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_export_pdf)))
        }
    }

    private fun exportRecipesAsJson() {
        viewModel.exportRecipes { json ->
            val file = File(cacheDir, "recipes.json")
            file.writeText(json)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_subject))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_export)))
        }
    }

    private fun importFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                } ?: return@launch
                viewModel.importRecipes(json) { count ->
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.import_success, count),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity, R.string.import_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { importFromUri(it) }
            Intent.ACTION_SEND -> intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                ?.let { importFromUri(it) }
        }
    }

    private fun showAddRecipeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_recipe, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.editRecipeName)
        val emojiContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.emojiContainer)

        val emojis = listOf("🍞","🥐","🥖","🥯","🫓","🧁","🎂","🍰","🍩","🍪","🧇","🥞","🥗","🍕","🫕","🥨","🍫","🌾","🔥")
        val selectedEmojis = mutableListOf<String>()

        val dp44 = (44 * resources.displayMetrics.density).toInt()
        val dp4  = (4  * resources.displayMetrics.density).toInt()
        val dp2  = (2  * resources.displayMetrics.density).toInt()

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

        fun refreshBackgrounds(all: List<android.widget.TextView>) {
            all.forEach { btn ->
                btn.background = if (btn.tag in selectedEmojis || (btn.tag == "" && selectedEmojis.isEmpty()))
                    makeSelectedBackground() else null
            }
        }

        fun toggleEmoji(e: String, all: List<android.widget.TextView>) {
            if (e == "") { selectedEmojis.clear() }
            else if (e in selectedEmojis) { selectedEmojis.remove(e) }
            else if (selectedEmojis.size < 2) { selectedEmojis.add(e) }
            refreshBackgrounds(all)
        }

        // "none" button
        val noneBtn = android.widget.TextView(this).apply {
            text = "—"
            tag = ""
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(dp44, dp44).also { it.marginEnd = dp4 }
            background = makeSelectedBackground() // selected by default (none)
        }

        val allButtons = mutableListOf(noneBtn)
        emojiContainer.addView(noneBtn)

        emojis.forEach { e ->
            val btn = android.widget.TextView(this).apply {
                text = e
                tag = e
                textSize = 24f
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(dp44, dp44).also { it.marginEnd = dp4 }
            }
            emojiContainer.addView(btn)
            allButtons.add(btn)
            btn.setOnClickListener { toggleEmoji(e, allButtons) }
        }
        noneBtn.setOnClickListener { toggleEmoji("", allButtons) }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_recipe)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameInput.text?.toString()?.trim()
                if (!name.isNullOrEmpty()) {
                    viewModel.addRecipe(name, selectedEmojis.joinToString(""))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
