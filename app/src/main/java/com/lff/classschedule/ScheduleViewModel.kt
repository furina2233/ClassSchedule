package com.lff.classschedule

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lff.classschedule.database.Adjustment
import com.lff.classschedule.database.CourseDao
import com.lff.classschedule.database.AdjustmentDao
import com.lff.classschedule.database.CourseMapper.toCourse
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.pojo.Lesson
import com.lff.classschedule.util.ColorUtil
import com.lff.classschedule.util.CourseTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleViewModel(
    private val courseDao: CourseDao,
    private val adjustmentDao: AdjustmentDao
) : ViewModel() {

    private val _courseSet = MutableLiveData<Set<Course>>()
    val courseSet: LiveData<Set<Course>> = _courseSet

    private val _courseColorMap = MutableLiveData<Map<Course, Int>>()
    val courseColorMap: LiveData<Map<Course, Int>> = _courseColorMap

    private val _maxWeeks = MutableLiveData<Int>()
    val maxWeeks: LiveData<Int> = _maxWeeks

    private val _firstMonday = MutableLiveData<LocalDate>()
    val firstMonday: LiveData<LocalDate> = _firstMonday

    private val _adjustments = MutableLiveData<List<Adjustment>>()
    val adjustments: LiveData<List<Adjustment>> = _adjustments

    private var maxWeeksValue: Int = 18
    private var startMonth: Int = 9
    private var startDay: Int = 1
    private var startTimesCsv: String = ""

    fun setConfig(maxWeeks: Int, month: Int, day: Int, startTimes: String) {
        maxWeeksValue = maxWeeks
        startMonth = month
        startDay = day
        startTimesCsv = startTimes
    }

    fun refreshData() {
        viewModelScope.launch {
            val entities = withContext(Dispatchers.IO) { courseDao.getAllCourses() }
            val courses = entities.map { it.toCourse() }.toSet()
            _courseSet.postValue(courses)

            ColorUtil.resetColorPool()
            val colorMap = mutableMapOf<Course, Int>()
            for (course in courses) {
                colorMap[course] = ColorUtil.getColor(course.name)
            }
            _courseColorMap.postValue(colorMap)

            _maxWeeks.postValue(maxWeeksValue)

            val today = LocalDate.now()
            var startDoc = LocalDate.of(today.year, startMonth, startDay)
            if (startDoc.isAfter(today.plusMonths(1))) {
                startDoc = startDoc.minusYears(1)
            }
            _firstMonday.postValue(startDoc.with(DayOfWeek.MONDAY))

            val startTimes = startTimesCsv.split(",")
            val todayDow = today.dayOfWeek.value
            val nowTime = LocalTime.now()

            val activeAdjustments = withContext(Dispatchers.IO) { adjustmentDao.getActiveAdjustments() }
            val stale = resolveStaleAdjustments(activeAdjustments, startTimes, todayDow, nowTime)
            if (stale.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    for (adj in stale) {
                        adjustmentDao.delete(adj)
                    }
                }
                val remaining = withContext(Dispatchers.IO) { adjustmentDao.getActiveAdjustments() }
                _adjustments.postValue(remaining)
            } else {
                _adjustments.postValue(activeAdjustments)
            }
        }
    }

    fun getCourses(): Set<Course> = _courseSet.value ?: emptySet()

    fun getColorMap(): Map<Course, Int> = _courseColorMap.value ?: emptyMap()

    fun getFirstMonday(): LocalDate? = _firstMonday.value

    fun buildLessonMapForWeek(
        context: Context,
        week: Int,
        courses: Set<Course>,
        firstMonday: LocalDate
    ): MutableMap<DayOfWeek, MutableList<Lesson>> {
        val map = mutableMapOf<DayOfWeek, MutableList<Lesson>>()
        for (dayOfWeek in 1..7) {
            val lessons = mutableListOf<Lesson>()
            val targetDate = firstMonday
                .plusWeeks((week - 1).toLong())
                .plusDays((dayOfWeek - 1).toLong())

            for (course in courses) {
                if (course.dayOfWeek == dayOfWeek &&
                    week >= course.startWeek &&
                    week <= course.endWeek
                ) {
                    val timeRange = CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                        context, course.startLesson, course.endLesson
                    )
                    val startTimeStr = timeRange.split("-")[0]
                    val (hour, minute) = startTimeStr.split(":").map { it.toInt() }
                    val lessonDateTime = targetDate.atTime(hour, minute)

                    lessons.add(
                        Lesson(
                            course.name,
                            course.startLesson,
                            course.endLesson,
                            DayOfWeek.of(course.dayOfWeek),
                            "${course.name} - \n${course.location}",
                            lessonDateTime,
                            course
                        )
                    )
                }
            }
            lessons.sortBy { it.startLesson }
            map[DayOfWeek.of(dayOfWeek)] = lessons
        }
        return map
    }

    fun applyAdjustments(
        lessonMap: MutableMap<DayOfWeek, MutableList<Lesson>>,
        adjustments: List<Adjustment>
    ): MutableMap<DayOfWeek, MutableList<Lesson>> {
        if (adjustments.isEmpty()) return lessonMap.mapValues { it.value.toMutableList() }.toMutableMap()

        val result = lessonMap.mapValues { it.value.toMutableList() }.toMutableMap()

        for (adj in adjustments) {
            val origDay = DayOfWeek.of(adj.originalDayOfWeek)
            val origLessons = result[origDay] ?: continue

            val matchIndex = origLessons.indexOfFirst {
                it.startLesson == adj.originalStartLesson &&
                        it.endLesson == adj.originalEndLesson
            }
            if (matchIndex < 0) continue

            val matched = origLessons.removeAt(matchIndex)

            val newDay = DayOfWeek.of(adj.newDayOfWeek)
            val adjustedLesson = matched.copy(
                dayOfWeek = newDay,
                startLesson = adj.newStartLesson,
                endLesson = adj.newEndLesson,
                description = "${matched.course.name} - \n${matched.course.location}"
            )
            result.getOrPut(newDay) { mutableListOf() }.add(adjustedLesson)
        }

        result.values.forEach { it.sortBy { lesson -> lesson.startLesson } }
        return result
    }

    companion object {
        fun resolveStaleAdjustments(
            adjustments: List<Adjustment>,
            startTimes: List<String>,
            todayDow: Int,
            nowTime: LocalTime
        ): List<Adjustment> {
            return adjustments.filter { adj ->
                if (adj.newDayOfWeek < todayDow) return@filter true
                if (adj.newDayOfWeek > todayDow) return@filter false
                val timeIdx = adj.newStartLesson - 1
                if (timeIdx >= startTimes.size) return@filter false
                val parts = startTimes[timeIdx].split(":")
                if (parts.size < 2) return@filter false
                val courseTime = LocalTime.of(
                    parts[0].toIntOrNull() ?: return@filter false,
                    parts[1].toIntOrNull() ?: return@filter false
                )
                nowTime.isAfter(courseTime) || nowTime == courseTime
            }
        }
    }
}
