package com.lff.classschedule.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses_table ORDER BY day_of_week ASC, start_lesson ASC")
    suspend fun getAllCourses(): List<CourseEntity>

    @Query("SELECT * FROM courses_table ORDER BY day_of_week ASC, start_lesson ASC")
    fun getAllCoursesBlocking(): List<CourseEntity>

    @Insert
    suspend fun insert(course: CourseEntity)

    @Insert
    suspend fun insertAll(courses: List<CourseEntity>)

    @Update
    suspend fun update(course: CourseEntity)

    @Query("DELETE FROM courses_table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM courses_table WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("UPDATE courses_table SET end_week = :newMax WHERE end_week = :oldMax")
    suspend fun syncSemesterLengthened(oldMax: Int, newMax: Int): Int

    @Query("UPDATE courses_table SET end_week = :newMax WHERE end_week > :newMax")
    suspend fun syncSemesterShortened(newMax: Int): Int
}
