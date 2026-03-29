package com.lff.classschedule.pojo

import java.time.DayOfWeek

data class Lesson(
    val name: String,
    val startLesson: Int,
    val endLesson: Int,
    val dayOfWeek: DayOfWeek,
    val description: String
)