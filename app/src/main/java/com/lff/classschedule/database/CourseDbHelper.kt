package com.lff.classschedule.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.content.contentValuesOf
import com.lff.classschedule.pojo.Course

class CourseDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val TAG = "CourseDbHelper"

        const val DATABASE_NAME = "ClassSchedule.db"
        const val DATABASE_VERSION = 2
        const val TABLE_NAME = "courses_table"

        const val COL_NAME = "course_name"
        const val COL_START_WEEK = "start_week"
        const val COL_END_WEEK = "end_week"
        const val COL_DAY_OF_WEEK = "day_of_week"
        const val COL_START_LESSON = "start_lesson"
        const val COL_END_LESSON = "end_lesson"
        const val COL_COURSE_LOCATION = "location"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT,
                $COL_START_WEEK INTEGER,
                $COL_END_WEEK INTEGER,
                $COL_DAY_OF_WEEK INTEGER,
                $COL_START_LESSON INTEGER,
                $COL_END_LESSON INTEGER,
                $COL_COURSE_LOCATION TEXT
            )
        """.trimIndent()

        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun queryAllCourses(): Map<Int, Course> {
        val courses = mutableMapOf<Int, Course>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null, null, null, null, null,
            "$COL_DAY_OF_WEEK ASC, $COL_START_LESSON ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getInt(c.getColumnIndexOrThrow("id"))
                val course = Course(
                    c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                    c.getInt(c.getColumnIndexOrThrow(COL_START_WEEK)),
                    c.getInt(c.getColumnIndexOrThrow(COL_END_WEEK)),
                    c.getInt(c.getColumnIndexOrThrow(COL_DAY_OF_WEEK)),
                    c.getInt(c.getColumnIndexOrThrow(COL_START_LESSON)),
                    c.getInt(c.getColumnIndexOrThrow(COL_END_LESSON)),
                    c.getString(c.getColumnIndexOrThrow(COL_COURSE_LOCATION))
                )
                Log.d(TAG, "查询到的课程：$course")
                courses[id] = course
            }
        }
        return courses
    }

    fun deleteCourseById(id: Int): Boolean {
        val db = writableDatabase
        return try {
            db.delete(TABLE_NAME, "id = ?", arrayOf(id.toString()))
            Log.d(TAG, "删除的课程的id：$id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除课程失败：${e.message}")
            false
        }
    }

    fun deleteCourseByIds(ids: List<Int>): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            for (id in ids) {
                db.delete(TABLE_NAME, "id = ?", arrayOf(id.toString()))
                Log.d(TAG, "删除的课程的id：$id")
            }
            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            Log.e(TAG, "批量删除课程失败：${e.message}")
            false
        } finally {
            db.endTransaction()
        }
    }

    fun addCourse(course: Course): Boolean {
        val db = writableDatabase
        return try {
            val values = contentValuesOf(
                COL_NAME to course.name,
                COL_START_WEEK to course.startWeek,
                COL_END_WEEK to course.endWeek,
                COL_DAY_OF_WEEK to course.dayOfWeek,
                COL_START_LESSON to course.startLesson,
                COL_END_LESSON to course.endLesson,
                COL_COURSE_LOCATION to course.location
            )
            db.insert(TABLE_NAME, null, values)
            Log.d(TAG, "添加的课程：$course")
            true
        } catch (e: Exception) {
            Log.e(TAG, "添加课程失败：${e.message}")
            false
        }
    }

    fun updateCourse(courseId: Int, course: Course): Boolean {
        val db = writableDatabase
        return try {
            db.update(TABLE_NAME, course.toContentValues(), "id = ?", arrayOf(courseId.toString()))
            Log.d(TAG, "更新的课程：$course")
            true
        } catch (e: Exception) {
            Log.e(TAG, "更新课程失败：${e.message}")
            false
        }
    }

    fun syncCoursesWhenSemesterLengthened(oldMax: Int, newMax: Int): Int {
        val db = writableDatabase
        return try {
            val values = contentValuesOf(COL_END_WEEK to newMax)
            db.update(TABLE_NAME, values, "$COL_END_WEEK = ?", arrayOf(oldMax.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "同步延长周数失败: ${e.message}")
            0
        }
    }

    fun syncCoursesWhenSemesterShortened(newMax: Int): Int {
        val db = writableDatabase
        return try {
            val values = contentValuesOf(COL_END_WEEK to newMax)
            db.update(TABLE_NAME, values, "$COL_END_WEEK > ?", arrayOf(newMax.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "同步缩短周数失败: ${e.message}")
            0
        }
    }
}