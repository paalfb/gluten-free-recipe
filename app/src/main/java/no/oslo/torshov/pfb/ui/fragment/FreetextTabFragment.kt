package no.oslo.torshov.pfb.ui.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.ui.viewmodel.RecipeDetailViewModel

class FreetextTabFragment : Fragment() {

    companion object {
        private const val ARG_TAB_POSITION = "tab_position"

        fun newInstance(tabPosition: Int) = FreetextTabFragment().apply {
            arguments = Bundle().apply { putInt(ARG_TAB_POSITION, tabPosition) }
        }
    }

    private val viewModel: RecipeDetailViewModel by activityViewModels()
    private var tabPosition = 2
    private var watcher: TextWatcher? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_freetext_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabPosition = arguments?.getInt(ARG_TAB_POSITION) ?: 2

        val editText = view.findViewById<EditText>(R.id.editTips)
        val fabDone = view.findViewById<FloatingActionButton>(R.id.fabDone)

        editText.setOnFocusChangeListener { _, hasFocus ->
            fabDone.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }

        fabDone.setOnClickListener {
            editText.clearFocus()
            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
        }

        viewModel.getFreetextLiveData(tabPosition).observe(viewLifecycleOwner) { text ->
            if (!editText.hasFocus()) {
                watcher?.let { editText.removeTextChangedListener(it) }
                editText.setText(text)
                watcher = buildWatcher(editText)
                editText.addTextChangedListener(watcher)
            }
        }

        watcher = buildWatcher(editText)
        editText.addTextChangedListener(watcher)
    }

    private fun buildWatcher(editText: EditText) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            viewModel.updateFreetextSilent(tabPosition, s?.toString() ?: "")
        }
    }
}
