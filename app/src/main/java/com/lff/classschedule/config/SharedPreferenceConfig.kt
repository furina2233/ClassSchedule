package com.lff.classschedule.config

import android.content.Context
import androidx.core.content.edit

object SharedPreferenceConfig {
    private const val PREF_NAME = "class_schedule_config"

    const val KEY_MAX_WEEKS = "max_weeks"
    const val KEY_MAX_LESSONS_PER_DAY = "max_lessons_per_day"
    const val KEY_DURATION_PER_LESSON = "duration_per_lesson"
    const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    const val KEY_IS_FIRST_ADD_COURSE = "is_first_add_course"
    const val KEY_START_TIMES = "start_times_csv"
    const val KEY_TERM_COMMENCEMENT_TIME_MONTH = "term_commencement_time_month"
    const val KEY_TERM_COMMENCEMENT_TIME_DAY = "term_commencement_time_day"

    private val defaultValuesMap: MutableMap<String, Any> = mutableMapOf()

    init {
        defaultValuesMap[KEY_MAX_WEEKS] = 18
        defaultValuesMap[KEY_MAX_LESSONS_PER_DAY] = 13
        defaultValuesMap[KEY_DURATION_PER_LESSON] = 45
        defaultValuesMap[KEY_IS_FIRST_LAUNCH] = true
        defaultValuesMap[KEY_IS_FIRST_ADD_COURSE] = true
        defaultValuesMap[KEY_START_TIMES] = "8:00,9:50,10:40,11:30,12:20,14:00,14:50,15:50,16:40,17:30,19:00,19:50,20:40"
        defaultValuesMap[KEY_TERM_COMMENCEMENT_TIME_MONTH] = 9
        defaultValuesMap[KEY_TERM_COMMENCEMENT_TIME_DAY] = 1
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getInt(context: Context, key: String): Int =
        getPrefs(context).getInt(key, defaultValuesMap[key] as Int)

    fun setInt(context: Context, key: String, value: Int) {
        getPrefs(context).edit { putInt(key, value) }
    }

    fun getString(context: Context, key: String): String =
        getPrefs(context).getString(key, defaultValuesMap[key] as String)?:""

    fun setString(context: Context, key: String, value: String) {
        getPrefs(context).edit { putString(key, value) }
    }

    fun getBoolean(context: Context, key: String): Boolean =
        getPrefs(context).getBoolean(key, defaultValuesMap[key] as Boolean)

    fun setBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit { putBoolean(key, value) }
    }
}