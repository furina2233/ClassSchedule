package com.lff.classschedule.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lff.classschedule.R
import com.lff.classschedule.viewmodel.ScheduleViewModel
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.database.AppDatabase
import com.lff.classschedule.pojo.Lesson
import com.lff.classschedule.util.ViewUtil
import java.time.DayOfWeek
import java.time.LocalDate

class WeekScheduleFragment : Fragment() {

    companion object {
        const val ARG_WEEK = "week_number"

        fun newInstance(week: Int): WeekScheduleFragment {
            return WeekScheduleFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WEEK, week)
                }
            }
        }
    }

    private var weekNumber: Int = 0
    private val viewModel: ScheduleViewModel by activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(requireContext().applicationContext)
                @Suppress("UNCHECKED_CAST")
                return ScheduleViewModel(db.courseDao(), db.adjustmentDao()) as T
            }
        }
    }
    private lateinit var lessonMap: MutableMap<DayOfWeek, MutableList<Lesson>>

    private lateinit var tvMonth: TextView
    private lateinit var dateTextViews: List<TextView>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_week_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weekNumber = arguments?.getInt(ARG_WEEK) ?: 1

        tvMonth = view.findViewById(R.id.tv_month)
        dateTextViews = listOf(
            view.findViewById(R.id.tv_monday_day),
            view.findViewById(R.id.tv_tuesday_day),
            view.findViewById(R.id.tv_wednesday_day),
            view.findViewById(R.id.tv_thursday_day),
            view.findViewById(R.id.tv_friday_day),
            view.findViewById(R.id.tv_saturday_day),
            view.findViewById(R.id.tv_sunday_day)
        )

        viewModel.firstMonday.observe(viewLifecycleOwner) {
            if (it != null) renderTimetable()
        }
        viewModel.courseSet.observe(viewLifecycleOwner) {
            if (viewModel.getFirstMonday() != null) renderTimetable()
        }
        viewModel.adjustments.observe(viewLifecycleOwner) {
            if (viewModel.getFirstMonday() != null) renderTimetable()
        }
    }

    fun renderTimetable() {
        val courses = viewModel.getCourses()
        val firstMonday = viewModel.getFirstMonday() ?: return
        val colorMap = viewModel.getColorMap()
        val adjustments = viewModel.adjustments.value ?: emptyList()

        lessonMap = viewModel.buildLessonMapForWeek(
            requireContext(), weekNumber, courses, firstMonday
        )
        lessonMap = viewModel.applyAdjustments(lessonMap, adjustments)

        loadClassScheduleTable()
        setupDividerLines()
        updateDateHeader(firstMonday)
        fillClassSchedule(colorMap)
    }

    private fun loadClassScheduleTable() {
        val llColumnHeader = requireView().findViewById<LinearLayout>(R.id.ll_column_header)
        llColumnHeader.removeAllViews()
        val maxLessons = SharedPreferenceConfig.getString(
            requireContext(), SharedPreferenceConfig.KEY_START_TIMES
        ).split(",").size
        for (i in 1..maxLessons) {
            val headerItem = layoutInflater.inflate(
                R.layout.item_class_schedule_column_header, llColumnHeader, false
            )
            headerItem.findViewById<TextView>(R.id.tv_column_header).text = "$i"
            llColumnHeader.addView(headerItem)
        }
    }

    private fun setupDividerLines() {
        val flRoot = requireView().findViewById<FrameLayout>(R.id.fl_timetable_root)
        val maxLessons = SharedPreferenceConfig.getString(
            requireContext(), SharedPreferenceConfig.KEY_START_TIMES
        ).split(",").size

        val singleSlotHeightPx = ViewUtil.dpToPx(requireContext(), ViewUtil.SINGLE_LESSON_DP)
        val marginBottomPx = ViewUtil.dpToPx(requireContext(), ViewUtil.MARGIN_BOTTOM_DP)
        val slotTotalHeightPx = singleSlotHeightPx + marginBottomPx
        val dividerHeightPx = ViewUtil.dpToPx(requireContext(), 1)

        for (i in 0..maxLessons) {
            val divider = View(requireContext()).apply {
                setBackgroundColor(resources.getColor(R.color.light_gray, null))
                alpha = 0.5f
            }
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dividerHeightPx
            )
            params.topMargin = i * slotTotalHeightPx
            flRoot.addView(divider, i, params)
        }
    }

    private fun updateDateHeader(firstMonday: LocalDate) {
        val currentMonday = firstMonday.plusWeeks((weekNumber - 1).toLong())
        tvMonth.text = "${currentMonday.monthValue}月"

        for (i in 0..6) {
            val dateOfRow = currentMonday.plusDays(i.toLong())
            dateTextViews[i].text = dateOfRow.dayOfMonth.toString()

            val parentLayout = dateTextViews[i].parent as LinearLayout
            val dayOfWeekTextView = parentLayout.getChildAt(0) as TextView
            val column = requireView().findViewById<LinearLayout>(R.id.ll_column_container).getChildAt(i)

            if (dateOfRow == LocalDate.now()) {
                dateTextViews[i].setTextColor(resources.getColor(R.color.high_light_foreground, null))
                parentLayout.setBackgroundColor(resources.getColor(R.color.high_light_background, null))
                dayOfWeekTextView.setTextColor(resources.getColor(R.color.high_light_foreground, null))
                column.setBackgroundColor(resources.getColor(R.color.high_light_background, null))
            } else {
                dateTextViews[i].setTextColor(resources.getColor(R.color.gray, null))
                dayOfWeekTextView.setTextColor(resources.getColor(R.color.title_text, null))
                parentLayout.setBackgroundColor(Color.TRANSPARENT)
                column.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun fillClassSchedule(colorMap: Map<com.lff.classschedule.pojo.Course, Int>) {
        val columnLayouts = listOf<LinearLayout>(
            requireView().findViewById(R.id.ll_monday_container),
            requireView().findViewById(R.id.ll_tuesday_container),
            requireView().findViewById(R.id.ll_wednesday_container),
            requireView().findViewById(R.id.ll_thursday_container),
            requireView().findViewById(R.id.ll_friday_container),
            requireView().findViewById(R.id.ll_saturday_container),
            requireView().findViewById(R.id.ll_sunday_container)
        )

        val maxLessons = SharedPreferenceConfig.getString(
            requireContext(), SharedPreferenceConfig.KEY_START_TIMES
        ).split(",").size

        for (i in 1..7) {
            val dayOfWeek = DayOfWeek.of(i)
            val dayLayout = columnLayouts[i - 1]
            dayLayout.removeAllViews()

            val dayLessons = lessonMap[dayOfWeek]?.sortedBy { it.startLesson } ?: emptyList()
            var currentLesson = 1

            for (lesson in dayLessons) {
                if (lesson.startLesson > currentLesson) {
                    val gapSize = lesson.startLesson - currentLesson
                    addEmptySlots(dayLayout, gapSize)
                }
                addCourseCard(dayLayout, lesson, colorMap)
                currentLesson = lesson.endLesson + 1
            }

            if (currentLesson <= maxLessons) {
                addEmptySlots(dayLayout, maxLessons - currentLesson + 1)
            }
        }
    }

    private fun addCourseCard(
        container: LinearLayout,
        lesson: Lesson,
        colorMap: Map<com.lff.classschedule.pojo.Course, Int>
    ) {
        val duration = lesson.endLesson - lesson.startLesson + 1

        val slotContainer = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewUtil.getLessonHeightPx(requireContext(), duration)
            ).apply {
                bottomMargin = ViewUtil.dpToPx(requireContext(), ViewUtil.MARGIN_BOTTOM_DP)
            }
        }

        val card = layoutInflater.inflate(
            R.layout.item_class_schedule_lesson_card, slotContainer, false
        )
        card.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        val tvLessonInfo = card.findViewById<TextView>(R.id.tv_lesson_info)
        tvLessonInfo.text = lesson.description

        val courseColor = colorMap[lesson.course]!!
        val flCard = card.findViewById<View>(R.id.fl_lesson_card)
        flCard.background.setTint(courseColor)

        card.setOnClickListener {
            val dialog = LessonInfoDialog.newInstance(lesson)
            dialog.show(parentFragmentManager, LessonInfoDialog.TAG)
        }

        slotContainer.addView(card)
        container.addView(slotContainer)
    }

    private fun addEmptySlots(container: LinearLayout, duration: Int) {
        val slotContainer = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewUtil.getLessonHeightPx(requireContext(), duration)
            ).apply {
                bottomMargin = ViewUtil.dpToPx(requireContext(), ViewUtil.MARGIN_BOTTOM_DP)
            }
        }

        container.addView(slotContainer)
    }
}
