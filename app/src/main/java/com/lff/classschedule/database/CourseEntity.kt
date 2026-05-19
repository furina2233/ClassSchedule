package com.lff.classschedule.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses_table")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "course_name")
    val name: String,
    @ColumnInfo(name = "start_week")
    val startWeek: Int,
    @ColumnInfo(name = "end_week")
    val endWeek: Int,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,
    @ColumnInfo(name = "start_lesson")
    val startLesson: Int,
    @ColumnInfo(name = "end_lesson")
    val endLesson: Int,
    @ColumnInfo(name = "location")
    val location: String
)
