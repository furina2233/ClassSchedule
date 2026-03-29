package com.lff.classschedule.util

import android.content.Context
import android.util.TypedValue

object ViewUtil {
    const val SINGLE_LESSON_DP = 70 // 基础高度
    const val MARGIN_DP = 1         // 单侧边距

    fun getLessonHeightPx(context: Context, duration: Int): Int {
        val totalHeight = duration * (SINGLE_LESSON_DP + 2*MARGIN_DP)
        return dpToPx(context, totalHeight)
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}