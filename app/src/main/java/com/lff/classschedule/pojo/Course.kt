package com.lff.classschedule.pojo

import android.content.ContentValues
import android.os.Parcelable
import com.lff.classschedule.database.CourseDbHelper
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
) : Parcelable {
    fun toContentValues(): ContentValues {
        return ContentValues().apply {
            put(CourseDbHelper.COL_NAME, name)
            put(CourseDbHelper.COL_START_WEEK, startWeek)
            put(CourseDbHelper.COL_END_WEEK, endWeek)
            put(CourseDbHelper.COL_DAY_OF_WEEK, dayOfWeek)
            put(CourseDbHelper.COL_START_LESSON, startLesson)
            put(CourseDbHelper.COL_END_LESSON, endLesson)
            put(CourseDbHelper.COL_COURSE_LOCATION, location)
        }
    }
}