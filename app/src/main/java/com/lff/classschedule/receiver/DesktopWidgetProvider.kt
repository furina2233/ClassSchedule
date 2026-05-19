package com.lff.classschedule.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lff.classschedule.MainActivity
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.database.AppDatabase
import com.lff.classschedule.database.CourseMapper.toCourse
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.util.CourseTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class DesktopWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "DesktopWidgetProvider"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_desktop)

        val db = AppDatabase.getInstance(context)
        val courseSet = runBlocking(Dispatchers.IO) {
            db.courseDao().getAllCoursesBlocking()
        }.map { it.toCourse() }.toMutableSet()

        val nextCourse = findNextLesson(context, courseSet)

        if (nextCourse != null) {
            views.setTextViewText(R.id.next_lesson_name, nextCourse.name)

            val timeStr = CourseTimeUtil.getFormatLessonTime(context, nextCourse)
            views.setTextViewText(R.id.next_lesson_time, timeStr)
            views.setTextViewText(R.id.next_lesson_location, nextCourse.location)
        } else {
            views.setTextViewText(R.id.next_lesson_name, context.getString(R.string.no_lesson))
            views.setTextViewText(R.id.next_lesson_time, context.getString(R.string.no_lesson))
            views.setTextViewText(R.id.next_lesson_location, context.getString(R.string.no_lesson))
        }

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.desktop_widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun findNextLesson(context: Context, courseSet: Set<Course>): Course? {
        val firstMonday = getFirstMonday(context)
        val currentWeek = calculateCurrentWeek(firstMonday)
        val maxWeeks = SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_MAX_WEEKS)

        val now = LocalDate.now()
        val currentTime = LocalTime.now()

        var searchWeek = currentWeek
        var searchDate = now

        while (searchWeek <= maxWeeks) {
            val coursesInThisWeek = courseSet.filter {
                it.startWeek <= searchWeek && it.endWeek >= searchWeek
            }

            if (coursesInThisWeek.isNotEmpty()) {
                var daysToCheck = 7
                var currentSearchDate = searchDate

                while (daysToCheck > 0) {
                    val currentSearchDayOfWeek = currentSearchDate.dayOfWeek.value

                    val coursesOnThisDay = coursesInThisWeek.filter {
                        it.dayOfWeek == currentSearchDayOfWeek
                    }.sortedBy { it.startLesson }

                    if (currentSearchDate.isEqual(now)) {
                        val futureCourses = coursesOnThisDay.filter { course ->
                            val startTime = CourseTimeUtil.getStartTime(context, course)
                            LocalTime.parse(startTime).isAfter(currentTime)
                        }

                        if (futureCourses.isNotEmpty()) {
                            return futureCourses.first()
                        }
                    } else if (currentSearchDate.isAfter(now)) {
                        if (coursesOnThisDay.isNotEmpty()) {
                            return coursesOnThisDay.first()
                        }
                    }

                    currentSearchDate = currentSearchDate.plusDays(1)
                    daysToCheck--
                }
            }

            searchWeek++
            searchDate = searchDate.plusWeeks(1)
        }

        return null
    }



    private fun getFirstMonday(context: Context): LocalDate {
        val startMonth = SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_MONTH)
        val startDay = SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_DAY)

        val today = LocalDate.now()
        var startDate = LocalDate.of(today.year, startMonth, startDay)
        if (startDate.isAfter(today.plusMonths(1))) {
            startDate = startDate.minusYears(1)
        }
        return startDate.with(DayOfWeek.MONDAY)
    }

    private fun calculateCurrentWeek(firstMonday: LocalDate): Int {
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(firstMonday, today)
        return (daysBetween / 7).toInt() + 1
    }
}
