package no.oslo.torshov.pfb.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.db.AppDatabase
import no.oslo.torshov.pfb.data.db.DayNoteDao
import no.oslo.torshov.pfb.data.model.DayNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DayNoteBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: String) = DayNoteBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_DATE, date) }
        }
    }

    private lateinit var noteEditText: EditText
    private lateinit var date: String
    private lateinit var dao: DayNoteDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_day_note, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        date = arguments?.getString(ARG_DATE) ?: return
        dao = AppDatabase.getInstance(requireContext()).dayNoteDao()

        noteEditText = view.findViewById(R.id.noteEditText)
        view.findViewById<TextView>(R.id.dateTitle).text = formatDateForDisplay(date)

        lifecycleScope.launch {
            val note = dao.getByDate(date)
            noteEditText.setText(note?.text ?: "")
            noteEditText.setSelection(noteEditText.text.length)
        }

        // Expand fully so keyboard has room
        dialog?.setOnShowListener {
            val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED }
        }
    }

    override fun onStop() {
        super.onStop()
        val text = noteEditText.text.toString()
        lifecycleScope.launch {
            dao.upsert(DayNote(date = date, text = text))
        }
    }

    private fun formatDateForDisplay(dateStr: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: Date()
            SimpleDateFormat("EEEE d. MMMM yyyy", Locale("no")).format(parsed)
                .replaceFirstChar { it.uppercaseChar() }
        } catch (e: Exception) {
            dateStr
        }
    }
}
