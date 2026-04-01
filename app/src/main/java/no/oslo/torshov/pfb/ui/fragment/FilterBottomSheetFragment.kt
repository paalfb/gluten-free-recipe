package no.oslo.torshov.pfb.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.ui.viewmodel.MainViewModel
import no.oslo.torshov.pfb.ui.viewmodel.StabiliserFilter

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val radioGroup = view.findViewById<RadioGroup>(R.id.stabiliserRadioGroup)
        radioGroup.check(filterToRadioId(viewModel.stabiliserFilter.value ?: StabiliserFilter.ALL))
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.stabiliserFilter.value = radioIdToFilter(checkedId)
        }
    }

    private fun filterToRadioId(filter: StabiliserFilter) = when (filter) {
        StabiliserFilter.ALL  -> R.id.radioAll
        StabiliserFilter.E415 -> R.id.radioE415
        StabiliserFilter.E464 -> R.id.radioE464
        StabiliserFilter.BOTH -> R.id.radioBoth
        StabiliserFilter.NONE -> R.id.radioNone
    }

    private fun radioIdToFilter(id: Int) = when (id) {
        R.id.radioE415 -> StabiliserFilter.E415
        R.id.radioE464 -> StabiliserFilter.E464
        R.id.radioBoth -> StabiliserFilter.BOTH
        R.id.radioNone -> StabiliserFilter.NONE
        else           -> StabiliserFilter.ALL
    }

    companion object {
        const val TAG = "FilterBottomSheet"
    }
}
