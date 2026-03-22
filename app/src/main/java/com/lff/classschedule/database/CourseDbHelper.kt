package com.lff.classschedule.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CourseDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "ClassSchedule.db"
        const val DATABASE_VERSION = 2
        const val TABLE_NAME = "courses_table"

        // 列名定义
        const val COL_NAME = "course_name"
        const val COL_START_WEEK = "start_week"
        const val COL_END_WEEK = "end_week"
        const val COL_DAY_OF_WEEK = "day_of_week"
        const val COL_START_LESSON = "start_lesson"
        const val COL_END_LESSON = "end_lesson"
        const val COL_COURSE_LOCATION = "location"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建表的 SQL 语句，使用 IF NOT EXISTS 确保安全
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
        // 简单处理：如果版本更新，删除旧表重新创建
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}