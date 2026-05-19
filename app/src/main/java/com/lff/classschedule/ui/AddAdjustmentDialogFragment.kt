package com.lff.classschedule.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.database.Adjustment
import com.lff.classschedule.database.AppDatabase
import com.lff.classschedule.database.CourseMapper.toCourse
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.util.ScreenUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AddAdjustmentDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "AddAdjustmentDialogFragment"
    }

    private data class CourseItem(
        val course: Course,
        val displayText: String
    ) {
        override fun toString(): String = displayText
    }

    private val dayOfWeekNames = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private var allCourses: Map<Int, Course> = emptyMap()
    private var currentWeek: Int = 1
    private var today: LocalDate = LocalDate.now()
    private var todayDow: Int = today.dayOfWeek.value  // 1=Mon..7=Sun
    private var now: LocalTime = LocalTime.now()
    private var maxLessons: Int = 0
    private lateinit var availableWeeks: List<Int>

    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_adjustment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        allCourses = runBlocking(Dispatchers.IO) {
            db.courseDao().getAllCourses()
        }.associate { it.id to it.toCourse() }
        val maxWeeks = SharedPreferenceConfig.getInt(
            requireContext(), SharedPreferenceConfig.KEY_MAX_WEEKS
        )
        maxLessons = SharedPreferenceConfig.getString(
            requireContext(), SharedPreferenceConfig.KEY_START_TIMES
        ).split(",").size

        currentWeek = calculateCurrentWeek()

        availableWeeks = (currentWeek..maxWeeks).toList()
        val weekItems = availableWeeks.map { "第${it}周" }

        val weekAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            weekItems
        )
        weekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinnerOrigWeek = view.findViewById<Spinner>(R.id.spinner_orig_week)
        val spinnerOrigCourse = view.findViewById<Spinner>(R.id.spinner_orig_course)
        val spinnerNewWeek = view.findViewById<Spinner>(R.id.spinner_new_week)
        val spinnerNewDay = view.findViewById<Spinner>(R.id.spinner_new_day)
        val spinnerNewStartLesson = view.findViewById<Spinner>(R.id.spinner_new_start_lesson)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_adjustment)

        spinnerOrigWeek.adapter = weekAdapter
        spinnerNewWeek.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            weekItems
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // 开始节数下拉框：1..maxLessons
        val lessonItems = (1..maxLessons).map { "第${it}节" }
        val lessonAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            lessonItems
        )
        lessonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNewStartLesson.adapter = lessonAdapter

        // 周几下拉框：始终显示全部
        val fullDayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            dayOfWeekNames.slice(1..7)
        )
        fullDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNewDay.adapter = fullDayAdapter

        val emptyCourseAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("请先选择周数")
        )
        emptyCourseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOrigCourse.adapter = emptyCourseAdapter

        spinnerOrigWeek.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedWeek = availableWeeks[position]
                populateCourseSpinner(spinnerOrigCourse, selectedWeek, selectedWeek == currentWeek)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        spinnerOrigCourse.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item = spinnerOrigCourse.selectedItem
                if (item !is CourseItem) return
                val course = item.course
                // 默认调后时间 = 原时间
                val origWeekPos = spinnerOrigWeek.selectedItemPosition
                spinnerNewWeek.setSelection(origWeekPos)
                spinnerNewDay.setSelection(course.dayOfWeek - 1)
                spinnerNewStartLesson.setSelection(course.startLesson - 1)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 默认选中当前周 → populateCourseSpinner → 选中下一节课
        spinnerOrigWeek.setSelection(0)

        btnSave.setOnClickListener {
            val origCourseItem = spinnerOrigCourse.selectedItem
            if (origCourseItem !is CourseItem) {
                Toast.makeText(requireContext(), "请选择要调的课", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val course = origCourseItem.course
            val origWeek = availableWeeks[spinnerOrigWeek.selectedItemPosition]
            val newWeek = availableWeeks[spinnerNewWeek.selectedItemPosition]
            val newDay = spinnerNewDay.selectedItemPosition + 1
            val newStart = spinnerNewStartLesson.selectedItemPosition + 1
            val duration = course.endLesson - course.startLesson + 1
            val newEnd = newStart + duration - 1

            if (newEnd > maxLessons) {
                Toast.makeText(requireContext(), "调课后的下课时间不能晚于最后一节课的下课时间！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 校验：调后上课时间不能早于当前时间（仅本周）
            if (newWeek == currentWeek) {
                val startTimes = SharedPreferenceConfig.getString(
                    requireContext(), SharedPreferenceConfig.KEY_START_TIMES
                ).split(",")
                if (isTimePassed(newDay, newStart, startTimes)) {
                    Toast.makeText(requireContext(), "调课后的上课时间不能晚于当前时间！", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 校验：调后时间不能和原时间一模一样
            if (newWeek == origWeek && newDay == course.dayOfWeek && newStart == course.startLesson) {
                Toast.makeText(requireContext(), "调课后的时间不能和原时间一样！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 校验：这节课是否已经上过了
            if (origWeek == currentWeek) {
                val startTimes = SharedPreferenceConfig.getString(
                    requireContext(), SharedPreferenceConfig.KEY_START_TIMES
                ).split(",")
                if (!hasCourseNotPassedYet(course, startTimes)) {
                    Toast.makeText(requireContext(), "这节课已经上过了", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 校验：这门课是否已经被调过
            val dao = AppDatabase.getInstance(requireContext()).adjustmentDao()
            val activeAdjustments = runBlocking(Dispatchers.IO) { dao.getActiveAdjustments() }
            val alreadyAdjusted = activeAdjustments.any {
                it.originalDayOfWeek == course.dayOfWeek &&
                        it.originalStartLesson == course.startLesson &&
                        it.originalEndLesson == course.endLesson
            }
            if (alreadyAdjusted) {
                Toast.makeText(requireContext(), "该课程已经被调过", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 校验：调后时间是否和已有课程冲突（排除自身）
            val targetDayCourses = allCourses.values.filter {
                it.dayOfWeek == newDay &&
                        newWeek >= it.startWeek &&
                        newWeek <= it.endWeek &&
                        !(it === course)
            }
            val hasCourseConflict = targetDayCourses.any {
                newStart <= it.endLesson && newEnd >= it.startLesson
            }
            if (hasCourseConflict) {
                Toast.makeText(requireContext(), "调整后的时间与已有课程冲突", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 校验：调后时间是否和已有调课记录的目标时间冲突
            val otherAdjustments = activeAdjustments.filter {
                !(it.originalDayOfWeek == course.dayOfWeek &&
                        it.originalStartLesson == course.startLesson &&
                        it.originalEndLesson == course.endLesson)
            }
            val hasAdjustmentConflict = otherAdjustments.any {
                it.newDayOfWeek == newDay &&
                        newStart <= it.newEndLesson && newEnd >= it.newStartLesson
            }
            if (hasAdjustmentConflict) {
                Toast.makeText(requireContext(), "调整后的时间与已有调课记录冲突", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val adjustment = Adjustment(
                description = origCourseItem.displayText,
                originalDayOfWeek = course.dayOfWeek,
                originalStartLesson = course.startLesson,
                originalEndLesson = course.endLesson,
                newDayOfWeek = newDay,
                newStartLesson = newStart,
                newEndLesson = newEnd
            )

            runBlocking(Dispatchers.IO) { dao.insert(adjustment) }
            parentFragmentManager.setFragmentResult(TAG, Bundle())
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun populateCourseSpinner(spinner: Spinner, week: Int, isCurrentWeek: Boolean) {
        val startTimes = SharedPreferenceConfig.getString(
            requireContext(), SharedPreferenceConfig.KEY_START_TIMES
        ).split(",")

        val coursesInWeek = allCourses.values.filter {
            week >= it.startWeek && week <= it.endWeek
        }.filter { course ->
            if (!isCurrentWeek) return@filter true
            hasCourseNotPassedYet(course, startTimes)
        }.map { course ->
            val dayText = dayOfWeekNames[course.dayOfWeek]
            val timeRange = "${course.startLesson}-${course.endLesson}"
            CourseItem(
                course = course,
                displayText = "$dayText 第${timeRange}节 ${course.name}"
            )
        }.sortedWith(compareBy({ it.course.dayOfWeek }, { it.course.startLesson }))

        if (coursesInWeek.isEmpty()) {
            val msg = if (isCurrentWeek) "本周剩余课程都已上过" else "该周无课程"
            val emptyAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listOf(msg)
            )
            emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = emptyAdapter
        } else {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                coursesInWeek
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    private fun hasCourseNotPassedYet(course: Course, startTimes: List<String>): Boolean {
        if (course.dayOfWeek < todayDow) return false
        if (course.dayOfWeek > todayDow) return true

        // 同一天：判断课程开始时间是否已过
        val timeIndex = course.startLesson - 1
        if (timeIndex >= startTimes.size) return false
        val startTimeStr = startTimes[timeIndex]
        val parts = startTimeStr.split(":")
        if (parts.size < 2) return false
        val courseTime = LocalTime.of(
            parts[0].toIntOrNull() ?: return false,
            parts[1].toIntOrNull() ?: return false
        )
        return now.isBefore(courseTime)
    }

    private fun isTimePassed(dayOfWeek: Int, startLesson: Int, startTimes: List<String>): Boolean {
        if (dayOfWeek < todayDow) return true
        if (dayOfWeek > todayDow) return false
        val timeIndex = startLesson - 1
        if (timeIndex >= startTimes.size) return true
        val parts = startTimes[timeIndex].split(":")
        if (parts.size < 2) return true
        val courseTime = LocalTime.of(
            parts[0].toIntOrNull() ?: return true,
            parts[1].toIntOrNull() ?: return true
        )
        return !now.isBefore(courseTime)
    }

    private fun calculateCurrentWeek(): Int {
        val startMonth = SharedPreferenceConfig.getInt(
            requireContext(), SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_MONTH
        )
        val startDay = SharedPreferenceConfig.getInt(
            requireContext(), SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_DAY
        )
        var startDoc = LocalDate.of(today.year, startMonth, startDay)
        if (startDoc.isAfter(today.plusMonths(1))) {
            startDoc = startDoc.minusYears(1)
        }
        val firstMonday = startDoc.with(DayOfWeek.MONDAY)
        val daysBetween = ChronoUnit.DAYS.between(firstMonday, today)
        return (daysBetween / 7).toInt() + 1
    }
}
