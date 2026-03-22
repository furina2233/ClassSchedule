package com.lff.classschedule.config

import android.content.Context
import androidx.core.content.edit

object SchoolScheduleConfig {
    private const val PREF_NAME = "class_schedule_config"
    private const val KEY_MAX_WEEKS = "max_weeks"
    private const val KEY_MAX_LESSONS = "max_lessons_per_day"
    private const val KEY_DURATION = "duration_per_lesson"

    private var cachedMaxWeeks: Int? = null
    private var cachedMaxLessons: Int? = null
    private var cachedDuration: Int? = null

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getMaxWeeksPerSemester(context: Context): Int =
        cachedMaxWeeks ?: getPrefs(context).getInt(KEY_MAX_WEEKS, 18).also { cachedMaxWeeks = it }

    fun setMaxWeeksPerSemester(context: Context, value: Int) {
        cachedMaxWeeks = value
        getPrefs(context).edit { putInt(KEY_MAX_WEEKS, value) }
    }

    fun getMaxLessonsPerDay(context: Context): Int =
        cachedMaxLessons ?: getPrefs(context).getInt(KEY_MAX_LESSONS, 13).also { cachedMaxLessons = it }

    fun setMaxLessonsPerDay(context: Context, value: Int) {
        cachedMaxLessons = value
        getPrefs(context).edit { putInt(KEY_MAX_LESSONS, value) }
    }

    fun getDurationPerLesson(context: Context): Int =
        cachedDuration ?: getPrefs(context).getInt(KEY_DURATION, 45)

    fun setDurationPerLesson(context: Context, value: Int) {
        cachedDuration = value
        getPrefs(context).edit { putInt(KEY_DURATION, value) }
    }

    fun getStartTimes(context: Context): Array<String> {
        val prefs = getPrefs(context)
        return prefs.getString("start_times_csv", null)?.split(",")?.toTypedArray()
            ?: arrayOf(
                "08:00",
                "08:50",
                "09:50",
                "10:40",
                "11:30",
                "14:00",
                "14:50",
                "15:50",
                "16:40",
                "17:30",
                "19:00",
                "19:50",
                "20:40"
            )
    }

    fun setStartTimes(context: Context, startTimes: Array<String>) {
        val prefs = getPrefs(context)
        prefs.edit { putString("start_times_csv", startTimes.joinToString(",")) }
    }

    fun clearCache() {
        cachedMaxWeeks = null
        cachedMaxLessons = null
        cachedDuration = null
    }
}