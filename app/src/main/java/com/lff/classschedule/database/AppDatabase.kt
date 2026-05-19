package com.lff.classschedule.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CourseEntity::class, Adjustment::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun adjustmentDao(): AdjustmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ClassScheduleUnified.db"
                )
                    .addCallback(MigrationCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

private class MigrationCallback(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        migrateOldCourseDb(context, db)
        migrateOldAdjustmentDb(context, db)
        context.deleteDatabase("ClassSchedule.db")
        context.deleteDatabase("ClassScheduleAdjustments.db")
    }

    private fun migrateOldCourseDb(context: Context, db: SupportSQLiteDatabase) {
        val oldDbFile = context.getDatabasePath("ClassSchedule.db")
        if (!oldDbFile.exists()) return

        val oldDb = SQLiteDatabase.openDatabase(oldDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = oldDb.rawQuery("SELECT * FROM courses_table", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val values = ContentValues().apply {
                    put("course_name", c.getString(c.getColumnIndexOrThrow("course_name")))
                    put("start_week", c.getInt(c.getColumnIndexOrThrow("start_week")))
                    put("end_week", c.getInt(c.getColumnIndexOrThrow("end_week")))
                    put("day_of_week", c.getInt(c.getColumnIndexOrThrow("day_of_week")))
                    put("start_lesson", c.getInt(c.getColumnIndexOrThrow("start_lesson")))
                    put("end_lesson", c.getInt(c.getColumnIndexOrThrow("end_lesson")))
                    put("location", c.getString(c.getColumnIndexOrThrow("location")))
                }
                db.insert("courses_table", 0, values)
            }
        }
        oldDb.close()
    }

    private fun migrateOldAdjustmentDb(context: Context, db: SupportSQLiteDatabase) {
        val oldDbFile = context.getDatabasePath("ClassScheduleAdjustments.db")
        if (!oldDbFile.exists()) return

        val oldDb = SQLiteDatabase.openDatabase(oldDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = oldDb.rawQuery("SELECT * FROM adjustments_table", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val values = ContentValues().apply {
                    put("description", c.getString(c.getColumnIndexOrThrow("description")))
                    put("originalDayOfWeek", c.getInt(c.getColumnIndexOrThrow("originalDayOfWeek")))
                    put("originalStartLesson", c.getInt(c.getColumnIndexOrThrow("originalStartLesson")))
                    put("originalEndLesson", c.getInt(c.getColumnIndexOrThrow("originalEndLesson")))
                    put("newDayOfWeek", c.getInt(c.getColumnIndexOrThrow("newDayOfWeek")))
                    put("newStartLesson", c.getInt(c.getColumnIndexOrThrow("newStartLesson")))
                    put("newEndLesson", c.getInt(c.getColumnIndexOrThrow("newEndLesson")))
                    put("newDate", c.getString(c.getColumnIndexOrThrow("newDate")))
                    put("isActive", c.getInt(c.getColumnIndexOrThrow("isActive")))
                }
                db.insert("adjustments_table", 0, values)
            }
        }
        oldDb.close()
    }
}
