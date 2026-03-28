package no.oslo.torshov.pfb.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import no.oslo.torshov.pfb.R

class CalendarTabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calendar_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val calendarView = view.findViewById<android.widget.CalendarView>(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val date = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            DayNoteBottomSheet.newInstance(date)
                .show(childFragmentManager, "day_note")
        }
    }
}
