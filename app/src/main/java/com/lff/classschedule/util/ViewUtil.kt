package com.lff.classschedule.util

import android.content.Context
import android.util.TypedValue

object ViewUtil {
    const val SINGLE_LESSON_DP = 70 // 基础高度

    fun getLessonHeightPx(context: Context, duration: Int): Int {
        val totalHeight = duration * SINGLE_LESSON_DP
        return dpToPx(context, totalHeight) + duration * 6
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}