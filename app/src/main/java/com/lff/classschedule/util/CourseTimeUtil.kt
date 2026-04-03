package com.lff.classschedule.util

import android.content.Context
import android.util.Log
import com.lff.classschedule.config.SharedPreferenceConfig

object CourseTimeUtil {
    private val TAG = "CourseTimeUtil"

    fun getTimeStringByStartAndEndClassIndex(context: Context, startIndex: Int, endIndex: Int): String {
        val duration = SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_DURATION_PER_LESSON)

        val startTimes = SharedPreferenceConfig.getString(context, SharedPreferenceConfig.KEY_START_TIMES)
            .split(",") as MutableList<String>

        try {
            val startTime = startTimes[startIndex - 1]
            val lastLessonStartTime = startTimes[endIndex - 1]
            val endTime = calculateEndTime(lastLessonStartTime, duration)
            return "$startTime-$endTime"
        } catch (e: IndexOutOfBoundsException) {
            // Toast.makeText(context, "请检查课程时间设置", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "请检查课程时间设置", e)
            return ""
        }
    }

    fun calculateEndTime(startTime: String, durationMinutes: Int): String {
        val parts = startTime.split(":")
        if (parts.size < 2) return startTime

        var hour = parts[0].toInt()
        var minute = parts[1].toInt()

        minute += durationMinutes

        // 处理进位
        if (minute >= 60) {
            hour += minute / 60
            minute %= 60
        }

        // 24小时制溢出处理（虽然课程通常不会上到半夜）
        if (hour >= 24) hour %= 24

        return String.format("%02d:%02d", hour, minute)
    }

    fun getDayOfWeekText(day: Int): String {
        return when (day) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> "未知"
        }
    }
}