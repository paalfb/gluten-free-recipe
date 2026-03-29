package no.oslo.torshov.pfb.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.db.AppDatabase
import no.oslo.torshov.pfb.data.model.RecipeExperience
import no.oslo.torshov.pfb.databinding.BottomSheetDayExperiencesBinding
import no.oslo.torshov.pfb.databinding.ItemDayExperienceBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DayExperiencesBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_DATE = "date"
        fun newInstance(date: String) = DayExperiencesBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_DATE, date) }
        }
    }

    private var _binding: BottomSheetDayExperiencesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetDayExperiencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val date = arguments?.getString(ARG_DATE) ?: return
        binding.tvTitle.text = formatDate(date)
        binding.recyclerExperiences.layoutManager = LinearLayoutManager(requireContext())
        loadExperiences(date)
    }

    private fun loadExperiences(date: String) {
        lifecycleScope.launch {
            val expDao = AppDatabase.getInstance(requireContext()).recipeExperienceDao()
            val recipeDao = AppDatabase.getInstance(requireContext()).recipeDao()
            val items = expDao.getByDate(date).map { exp ->
                (recipeDao.getById(exp.recipeId)?.name ?: "?") to exp
            }.toMutableList()

            binding.recyclerExperiences.adapter = DayExperienceAdapter(
                items = items,
                onEdit = { exp -> showEditDialog(exp, date) },
                onDelete = { exp, index ->
                    lifecycleScope.launch {
                        expDao.delete(exp)
                        (binding.recyclerExperiences.adapter as? DayExperienceAdapter)?.removeAt(index)
                        binding.tvEmpty.visibility = if (items.size == 1) View.VISIBLE else View.GONE
                    }
                }
            )
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showEditDialog(experience: RecipeExperience, date: String) {
        val layout = TextInputLayout(requireContext()).apply {
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

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(formatDate(experience.date))
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                lifecycleScope.launch {
                    val expDao = AppDatabase.getInstance(requireContext()).recipeExperienceDao()
                    expDao.update(experience.copy(note = input.text?.toString()?.trim() ?: ""))
                    loadExperiences(date)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatDate(dateStr: String): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: Date()
        SimpleDateFormat("EEEE d. MMMM yyyy", Locale("no")).format(parsed)
            .replaceFirstChar { it.uppercaseChar() }
    } catch (_: Exception) { dateStr }
}

class DayExperienceAdapter(
    private val items: MutableList<Pair<String, RecipeExperience>>,
    private val onEdit: (RecipeExperience) -> Unit,
    private val onDelete: (RecipeExperience, Int) -> Unit
) : RecyclerView.Adapter<DayExperienceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDayExperienceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemDayExperienceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (recipeName, exp) = items[position]
        holder.binding.tvRecipeName.text = recipeName
        if (exp.note.isNotBlank()) {
            holder.binding.tvNote.visibility = View.VISIBLE
            holder.binding.tvNote.text = exp.note
        } else {
            holder.binding.tvNote.visibility = View.GONE
        }
        holder.binding.root.setOnClickListener { onEdit(exp) }
        holder.binding.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                MaterialAlertDialogBuilder(holder.itemView.context)
                    .setTitle(R.string.delete_confirm_title)
                    .setMessage(holder.itemView.context.getString(R.string.delete_confirm_message, recipeName))
                    .setPositiveButton(R.string.delete) { _, _ -> onDelete(exp, pos) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    override fun getItemCount() = items.size

    fun removeAt(index: Int) {
        items.removeAt(index)
        notifyItemRemoved(index)
    }
}
