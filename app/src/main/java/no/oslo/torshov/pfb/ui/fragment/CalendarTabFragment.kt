package no.oslo.torshov.pfb.ui.fragment

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.launch
import no.oslo.torshov.pfb.R
import no.oslo.torshov.pfb.data.db.AppDatabase
import no.oslo.torshov.pfb.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class CalendarTabFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var calendarView: CalendarView
    private var experienceDates: Set<LocalDate> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calendar_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        calendarView = view.findViewById(R.id.calendarView)
        setupCalendar()
        loadExperienceDates()

        viewModel.pendingCalendarDate.observe(viewLifecycleOwner) { date ->
            if (date != null) {
                try {
                    val local = LocalDate.parse(date)
                    calendarView.scrollToMonth(YearMonth.of(local.year, local.month))
                    val intent = android.content.Intent(
                        requireContext(),
                        no.oslo.torshov.pfb.ui.DateExperiencesActivity::class.java
                    )
                    intent.putExtra(no.oslo.torshov.pfb.ui.DateExperiencesActivity.EXTRA_DATE, date)
                    startActivity(intent)
                } catch (_: Exception) { /* ignore */ }
                viewModel.pendingCalendarDate.value = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadExperienceDates()
    }

    private fun loadExperienceDates() {
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.getInstance(requireContext()).recipeExperienceDao()
            experienceDates = dao.getAllExperienceDates().mapNotNull { dateStr ->
                try {
                    LocalDate.parse(dateStr)
                } catch (_: Exception) {
                    null
                }
            }.toSet()
            calendarView.notifyCalendarChanged()
        }
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val fragment = this

        class DayViewContainer(view: View) : ViewContainer(view) {
            val tvDay: TextView = view.findViewById(R.id.tvDay)
            val dotExperience: View = view.findViewById(R.id.dotExperience)
            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        val intent = android.content.Intent(
                            fragment.requireContext(),
                            no.oslo.torshov.pfb.ui.DateExperiencesActivity::class.java
                        )
                        intent.putExtra(no.oslo.torshov.pfb.ui.DateExperiencesActivity.EXTRA_DATE, day.date.toString())
                        fragment.startActivity(intent)
                    }
                }
            }
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.tvDay.text = data.date.dayOfMonth.toString()

                val ctx = container.tvDay.context
                val isCurrentMonth = data.position == DayPosition.MonthDate
                val isToday = data.date == LocalDate.now()
                val hasExperience = data.date in experienceDates

                container.tvDay.alpha = if (isCurrentMonth) 1f else 0.3f

                if (isToday && isCurrentMonth) {
                    val color = MaterialColors.getColor(
                        ctx,
                        com.google.android.material.R.attr.colorPrimary,
                        0
                    )
                    container.tvDay.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(color)
                    }
                    container.tvDay.setTextColor(
                        MaterialColors.getColor(
                            ctx,
                            com.google.android.material.R.attr.colorOnPrimary,
                            0xFFFFFF
                        )
                    )
                } else {
                    container.tvDay.background = null
                    container.tvDay.setTextColor(
                        MaterialColors.getColor(
                            ctx,
                            if (isCurrentMonth) com.google.android.material.R.attr.colorOnSurface
                            else com.google.android.material.R.attr.colorOutline,
                            0
                        )
                    )
                }

                if (hasExperience && isCurrentMonth) {
                    val dotColor = MaterialColors.getColor(
                        ctx,
                        com.google.android.material.R.attr.colorSecondary,
                        0
                    )
                    (container.dotExperience.background as? GradientDrawable)?.setColor(dotColor)
                        ?: run {
                            container.dotExperience.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(dotColor)
                            }
                        }
                    container.dotExperience.visibility = View.VISIBLE
                } else {
                    container.dotExperience.visibility = View.INVISIBLE
                }
            }
        }

        class MonthHeaderContainer(view: View) : ViewContainer(view) {
            val tvMonth: TextView = view.findViewById(R.id.tvMonth)
        }

        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthHeaderContainer> {
            override fun create(view: View) = MonthHeaderContainer(view)
            override fun bind(container: MonthHeaderContainer, data: CalendarMonth) {
                val month = data.yearMonth.month
                    .getDisplayName(TextStyle.FULL, Locale.getDefault())
                    .replaceFirstChar { it.uppercase() }
                container.tvMonth.text = buildString {
                    append(month)
                    append(" ")
                    append(data.yearMonth.year)
                }
            }
        }

        calendarView.setup(startMonth, endMonth, firstDayOfWeekFromLocale())
        calendarView.scrollToMonth(currentMonth)
    }
}
