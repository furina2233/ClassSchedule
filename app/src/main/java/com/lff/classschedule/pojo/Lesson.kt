package com.lff.classschedule.pojo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.DayOfWeek
import java.time.LocalDateTime

@Parcelize
data class Lesson(
    val name: String,
    val startLesson: Int,
    val endLesson: Int,
    val dayOfWeek: DayOfWeek,
    val description: String,
    val startTime: LocalDateTime,
    val course: Course
) : Parcelable