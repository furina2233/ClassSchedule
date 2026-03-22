package com.lff.classschedule.pojo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Course(
    val name: String,
    val startWeek: Int,
    val endWeek: Int,
    val dayOfWeek: Int,
    val startLesson: Int,
    val endLesson: Int,
    val location: String
) : Parcelable