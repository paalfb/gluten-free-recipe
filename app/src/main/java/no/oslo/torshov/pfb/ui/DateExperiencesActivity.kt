package no.oslo.torshov.pfb.ui

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

class DateExperiencesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATE = "date"
    }

    private lateinit var binding: ActivityExperiencesBinding
    private lateinit var adapter: DateExperienceAdapter
    private lateinit var date: String

    private val dao by lazy { AppDatabase.getInstance(this).recipeExperienceDao() }
    private val recipeDao by lazy { AppDatabase.getInstance(this).recipeDao() }
    private val displayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExperiencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        date = intent.getStringExtra(EXTRA_DATE) ?: run { finish(); return }

        val formattedDate = try {
            LocalDate.parse(date).format(displayFormatter)
        } catch (_: Exception) { date }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.experiences_title)
        supportActionBar?.subtitle = formattedDate
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = DateExperienceAdapter(
            displayFormatter = displayFormatter,
            onEdit = { exp -> showEditDialog(exp) },
            onDelete = { exp ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_confirm_title)
                    .setMessage(getString(R.string.delete_confirm_message, exp.date))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        lifecycleScope.launch {
                            dao.delete(exp)
                            loadExperiences()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )

        binding.recyclerExperiences.layoutManager = LinearLayoutManager(this)
        binding.recyclerExperiences.adapter = adapter
        binding.fabAdd.visibility = View.GONE

        loadExperiences()
    }

    private val swipeDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                if (e1 != null && vX > 600 && abs(e2.x - e1.x) > 100 && abs(e2.y - e1.y) < 200) {
                    finish(); return true
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
            val experiences = dao.getByDate(date)
            adapter.submitList(experiences)
            binding.textEmpty.visibility = if (experiences.isEmpty()) View.VISIBLE else View.GONE
        }
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
            .setTitle(try { LocalDate.parse(experience.date).format(displayFormatter) } catch (_: Exception) { experience.date })
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
}

class DateExperienceAdapter(
    private val displayFormatter: DateTimeFormatter,
    private val onEdit: (RecipeExperience) -> Unit,
    private val onDelete: (RecipeExperience) -> Unit
) : RecyclerView.Adapter<DateExperienceAdapter.ViewHolder>() {

    private var items: List<RecipeExperience> = emptyList()
    private val expandedIds = mutableSetOf<Long>()

    fun submitList(list: List<RecipeExperience>) {
        items = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemExperienceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemExperienceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exp = items[position]
        holder.binding.textDate.text = exp.note.ifBlank { exp.date }
        val isExpanded = exp.id in expandedIds

        if (exp.note.isNotBlank()) {
            holder.binding.textNote.visibility = View.VISIBLE
            holder.binding.textNote.text = exp.note
            holder.binding.textNote.maxLines = if (isExpanded) Int.MAX_VALUE else 1
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
