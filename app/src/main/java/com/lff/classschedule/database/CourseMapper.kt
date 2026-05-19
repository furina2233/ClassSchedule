package com.lff.classschedule.database

import com.lff.classschedule.pojo.Course

object CourseMapper {

    fun CourseEntity.toCourse(): Course = Course(
        name = name,
        startWeek = startWeek,
        endWeek = endWeek,
        dayOfWeek = dayOfWeek,
        startLesson = startLesson,
        endLesson = endLesson,
        location = location
    )

    fun Course.toEntity(): CourseEntity = CourseEntity(
        name = name,
        startWeek = startWeek,
        endWeek = endWeek,
        dayOfWeek = dayOfWeek,
        startLesson = startLesson,
        endLesson = endLesson,
        location = location
    )

    fun Map<Int, CourseEntity>.toCourseMap(): Map<Int, Course> =
        mapValues { it.value.toCourse() }
}
