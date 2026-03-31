package no.oslo.torshov.pfb.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.db.AppDatabase
import no.oslo.torshov.pfb.data.model.RecipeExperience
import no.oslo.torshov.pfb.databinding.ActivityExperiencesBinding
import no.oslo.torshov.pfb.databinding.ItemExperienceBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

class ExperiencesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RECIPE_ID = "recipe_id"
        const val EXTRA_RECIPE_NAME = "recipe_name"
        const val RESULT_OPEN_CALENDAR_DATE = "open_calendar_date"
    }

    private lateinit var binding: ActivityExperiencesBinding
    private lateinit var adapter: ExperienceAdapter
    private var recipeId: Long = -1

    private val dao by lazy { AppDatabase.getInstance(this).recipeExperienceDao() }
    private val displayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.getDefault())
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExperiencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recipeId = intent.getLongExtra(EXTRA_RECIPE_ID, -1)
        val recipeName = intent.getStringExtra(EXTRA_RECIPE_NAME) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.experiences_title)
        supportActionBar?.subtitle = recipeName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ExperienceAdapter(
            displayFormatter = displayFormatter,
            onEdit = { experience -> showEditDialog(experience) },
            onDelete = { experience ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_confirm_title)
                    .setMessage(getString(R.string.delete_confirm_message, experience.date))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        lifecycleScope.launch {
                            dao.delete(experience)
                            loadExperiences()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )

        binding.recyclerExperiences.layoutManager = LinearLayoutManager(this)
        binding.recyclerExperiences.adapter = adapter

        binding.fabAdd.setOnClickListener { showAddDialog() }

        loadExperiences()
    }

    private val swipeDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                if (e1 != null && vX > 600 && abs(e2.x - e1.x) > 100 && abs(e2.y - e1.y) < 200) {
                    finish()
                    return true
                }
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        swipeDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun loadExperiences() {
        lifecycleScope.launch {
            val experiences = dao.getForRecipe(recipeId)
            adapter.submitList(experiences)
            binding.textEmpty.visibility = if (experiences.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddDialog() {
        val today = LocalDate.now()
        DatePickerDialog(this, { _, year, month, day ->
            val date = LocalDate.of(year, month + 1, day).format(isoFormatter)
            showNoteDialog(date)
        }, today.year, today.monthValue - 1, today.dayOfMonth).show()
    }

    private fun showEditDialog(experience: RecipeExperience) {
        val layout = TextInputLayout(this).apply {
            hint = getString(R.string.experience_note_hint)
            setPadding(48, 16, 48, 0)
        }
        val input = TextInputEditText(layout.context).apply {
            isSingleLine = false
            minLines = 3
            setText(experience.note)
            setSelection(text?.length ?: 0)
        }
        layout.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle(LocalDate.parse(experience.date, isoFormatter).format(displayFormatter))
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                lifecycleScope.launch {
                    dao.update(experience.copy(note = input.text?.toString()?.trim() ?: ""))
                    loadExperiences()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showNoteDialog(date: String) {
        val layout = TextInputLayout(this).apply {
            hint = getString(R.string.experience_note_hint)
            setPadding(48, 16, 48, 0)
        }
        val input = TextInputEditText(layout.context)
        input.isSingleLine = false
        input.minLines = 3
        layout.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle(LocalDate.parse(date, isoFormatter).format(displayFormatter))
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                lifecycleScope.launch {
                    dao.insert(RecipeExperience(
                        recipeId = recipeId,
                        date = date,
                        note = input.text?.toString()?.trim() ?: ""
                    ))
                    loadExperiences()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

class ExperienceAdapter(
    private val displayFormatter: DateTimeFormatter,
    private val onEdit: (RecipeExperience) -> Unit,
    private val onDelete: (RecipeExperience) -> Unit
) : RecyclerView.Adapter<ExperienceAdapter.ViewHolder>() {

    private var items: List<RecipeExperience> = emptyList()

    fun submitList(list: List<RecipeExperience>) {
        val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = list.size
            override fun areItemsTheSame(o: Int, n: Int) = items[o].id == list[n].id
            override fun areContentsTheSame(o: Int, n: Int) = items[o] == list[n]
        })
        items = list
        diff.dispatchUpdatesTo(this)
    }

    private val expandedIds = mutableSetOf<Long>()

    inner class ViewHolder(val binding: ItemExperienceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExperienceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exp = items[position]
        val localDate = try {
            LocalDate.parse(exp.date, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) { null }

        holder.binding.textDate.text = localDate?.format(displayFormatter) ?: exp.date

        val isExpanded = exp.id in expandedIds
        if (exp.note.isNotBlank()) {
            holder.binding.textNote.visibility = View.VISIBLE
            holder.binding.textNote.text = exp.note
            holder.binding.textNote.maxLines = if (isExpanded) Int.MAX_VALUE else 1
            holder.binding.textNote.requestLayout()
        } else {
            holder.binding.textNote.visibility = View.GONE
        }
        holder.binding.btnEdit.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (isExpanded) {
                expandedIds.remove(exp.id)
                notifyItemChanged(pos)
            } else {
                val previousId = expandedIds.firstOrNull()
                expandedIds.clear()
                expandedIds.add(exp.id)
                previousId?.let { prevId ->
                    val prevPos = items.indexOfFirst { it.id == prevId }
                    if (prevPos >= 0) notifyItemChanged(prevPos)
                }
                notifyItemChanged(pos)
            }
        }
        holder.binding.btnEdit.setOnClickListener { onEdit(exp) }
        holder.binding.btnDelete.setOnClickListener { onDelete(exp) }
    }

    override fun getItemCount() = items.size
}
