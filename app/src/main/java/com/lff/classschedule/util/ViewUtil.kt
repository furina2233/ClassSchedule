package com.lff.classschedule.util

import android.content.Context
import android.util.TypedValue
import com.lff.classschedule.R

object ViewUtil {
    private var SINGLE_LESSON_DP = -1 // 基础高度
    private var MARGIN_DP = -1        // 单侧边距

    fun getLessonHeightPx(context: Context, duration: Int): Int {
        if (SINGLE_LESSON_DP == -1 || MARGIN_DP == -1){
            SINGLE_LESSON_DP = pxToDp(context, context.resources.getDimensionPixelSize(R.dimen.default_header_and_card_height))
            MARGIN_DP = pxToDp(context, context.resources.getDimensionPixelSize(R.dimen.default_card_margin))
        }
        // 高度的边距是左右边距的2倍，所以在这里乘以2x2
        val totalHeight = duration * (SINGLE_LESSON_DP + 4 * MARGIN_DP)
        return dpToPx(context, totalHeight)
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    fun pxToDp(context: Context, px: Int): Int {
        val density = context.resources.displayMetrics.density
        return (px / density).toInt()
    }
}