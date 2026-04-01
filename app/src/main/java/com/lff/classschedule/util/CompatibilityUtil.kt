package com.lff.classschedule.util

import android.os.Build
import android.os.Bundle
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.pojo.Lesson

object CompatibilityUtil {
    fun getParcelableCourse(bundle: Bundle): Course? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return bundle.getParcelable("course", Course::class.java)
        } else {
            return bundle.getParcelable("course")
        }
    }

    fun getParcelableCourseList(bundle: Bundle): List<Course>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return bundle.getParcelableArrayList("course_list", Course::class.java)
        } else {
            return bundle.getParcelableArrayList("course_list")
        }
    }

    fun getParcelableLesson(bundle: android.os.Bundle): Lesson? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return bundle.getParcelable("lesson", Lesson::class.java)
        } else {
            return bundle.getParcelable("lesson")
        }
    }
}