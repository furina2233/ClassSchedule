package com.lff.classschedule

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lff.classschedule.config.SchoolScheduleConfig
import com.lff.classschedule.database.CourseDbHelper
import com.lff.classschedule.database.CourseDbHelper.Companion.TABLE_NAME
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.ui.AddCourseDialog
import com.lff.classschedule.ui.CustomSchoolScheduleDialog
import com.lff.classschedule.util.CompatibilityUtil
import com.lff.classschedule.util.CourseTimeUtil

class CoursesSettingActivity : AppCompatActivity() {

    val TAG = "CoursesSettingActivity"

    private lateinit var dbHelper: CourseDbHelper

    private lateinit var btnAddCourse: FloatingActionButton
    private lateinit var btnCourseSetting: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courses_setting)

        dbHelper = CourseDbHelper(this)
        val db = dbHelper.writableDatabase
        if (db.isOpen) {
            Log.d(TAG, "数据库已就绪，表 $TABLE_NAME 已检查/创建")
        }

        // 从课程设置页面返回时一定返回主页
        onBackPressedDispatcher.addCallback {
            val intent = Intent(this@CoursesSettingActivity, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            Log.d(TAG, "已返回主界面")
            finish()
        }

        // 插入一条随机数据，便于调试
//        val countCursor = db.rawQuery("SELECT count(*) FROM ${CourseDbHelper.TABLE_NAME}", null)
//        countCursor.moveToFirst()
//        val count = countCursor.getInt(0)
//        countCursor.close()
//        if (count == 0) {
//            val values = ContentValues().apply {
//                put(CourseDbHelper.COL_NAME, "高等数学 (调试用)")
//                put(CourseDbHelper.COL_START_WEEK, 1)
//                put(CourseDbHelper.COL_END_WEEK, 16)
//                put(CourseDbHelper.COL_DAY_OF_WEEK, 1) // 周一
//                put(CourseDbHelper.COL_COURSE_TIME, "08:00-09:35")
//                put(CourseDbHelper.COL_COURSE_LOCATION, "教 1-101")
//            }
//            db.insert(CourseDbHelper.TABLE_NAME, null, values)
//            Log.d(TAG, "已插入一条测试数据")
//        }

        loadCoursesFromDb()

        btnAddCourse = findViewById(R.id.btn_add_course)
        btnAddCourse.setOnClickListener {
            setupAddCourseButton()
        }

        btnCourseSetting = findViewById(R.id.btn_course_setting)
        btnCourseSetting.setOnClickListener {
            setupCourseSettingButton()
        }

        syncCoursesMaxWeeksIfNeed()
    }

    private fun syncCoursesMaxWeeksIfNeed() {
        supportFragmentManager.setFragmentResultListener(CustomSchoolScheduleDialog.TAG, this) { _, bundle ->
            val db = dbHelper.writableDatabase
            val needSync = bundle.getBoolean("need_sync")
            val newMaxWeeks = SchoolScheduleConfig.getMaxWeeksPerSemester(this)
            val oldMaxWeeks = bundle.getInt("old_max_weeks")

            val values = ContentValues()

            if (newMaxWeeks > oldMaxWeeks) {
                // 如果学期变长了，只有在用户点“好的”的情况下才同步
                if (needSync) {
                    values.put(CourseDbHelper.COL_END_WEEK, newMaxWeeks)
                    val rows = db.update(
                        TABLE_NAME,
                        values,
                        "${CourseDbHelper.COL_END_WEEK} = ?",
                        arrayOf(oldMaxWeeks.toString())
                    )
                    Toast.makeText(this, "已同步课程的持续时长", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "学期变长：已将 $rows 门全学期课程更新至 $newMaxWeeks 周")
                }
            } else {
                // 如果学期变短了，则强制更新所有持续到超出当前总周数的课程的持续时长
                values.put(CourseDbHelper.COL_END_WEEK, newMaxWeeks)
                val rows = db.update(
                    TABLE_NAME,
                    values,
                    "${CourseDbHelper.COL_END_WEEK} > ?",
                    arrayOf(newMaxWeeks.toString())
                )
                Log.d(TAG, "学期变短：已强制修正 $rows 门溢出课程的持续时长")
            }
            loadCoursesFromDb()
        }
    }

    private fun setupCourseSettingButton() {
        val popup = PopupMenu(this, btnCourseSetting)
        popup.menuInflater.inflate(R.menu.course_setting_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_batch_add -> {
                    TODO("批量添加课程")
                    true
                }

                R.id.menu_batch_delete -> {
                    TODO("批量删除课程")
                    true
                }

                R.id.menu_custom_school_schedule -> {
                    setCustomSchoolSchedule()
                    true
                }

                R.id.menu_set_start_times -> {
                    TODO("设置每节课上课时间")
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun setCustomSchoolSchedule() {
        val dialog = CustomSchoolScheduleDialog()
        dialog.show(supportFragmentManager, "CustomSchoolScheduleDialog")
    }

    private fun loadCoursesFromDb() {
        val container = findViewById<LinearLayout>(R.id.ll_course_list)
        container.removeAllViews() // 先清空，防止重复添加
        findViewById<TextView>(R.id.tv_no_courses).visibility = View.GONE

        val db = dbHelper.readableDatabase
        // 查询所有课程
        val cursor = db.query(
            TABLE_NAME,
            null, null, null, null, null,
            "${CourseDbHelper.COL_DAY_OF_WEEK} ASC, ${CourseDbHelper.COL_START_LESSON} ASC" // 先按星期几排，再按上课时间排
        )

        if (cursor.moveToFirst()) {
            Log.d(TAG, "数据库中有课程")
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val course = Course(
                    cursor.getString(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_NAME)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_START_WEEK)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_END_WEEK)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_DAY_OF_WEEK)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_START_LESSON)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_END_LESSON)),
                    cursor.getString(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_COURSE_LOCATION))
                )

                // 加载卡片布局
                val itemView = LayoutInflater.from(this).inflate(R.layout.item_course_card, container, false)

                val btnMore = itemView.findViewById<ImageButton>(R.id.btn_more)
                setupPopupMenu(btnMore, id, course)

                itemView.findViewById<TextView>(R.id.tv_course_name).text = course.name

                // 格式化周数：1-18 周
                val weekText = "${course.startWeek}-${course.endWeek} 周"
                itemView.findViewById<TextView>(R.id.tv_course_weeks).text = weekText

                // 格式化时间：周一 9:50-12:15
                val timeText = CourseTimeUtil.getDayOfWeekText(course.dayOfWeek) + "  " +
                        CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                            this, course.startLesson, course.endLesson
                        )
                itemView.findViewById<TextView>(R.id.tv_course_time).text = timeText

                itemView.findViewById<TextView>(R.id.tv_course_location).text = course.location

                // 将卡片放入滚动视图的容器中
                container.addView(itemView)
            } while (cursor.moveToNext())
        } else {
            Log.d(TAG, "数据库中无课程")
            findViewById<TextView>(R.id.tv_no_courses).visibility = View.VISIBLE
        }
        cursor.close()
        Log.d(TAG, "已从数据库加载课程数据")
    }

    private fun setupPopupMenu(btnMore: ImageButton, courseId: Int, course: Course) {
        btnMore.setOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menuInflater.inflate(R.menu.course_item_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_edit -> {
                        showEditCourseDialog(courseId, course) // 优化：直接把已有对象传过去，不用再查数据库
                        true
                    }

                    R.id.menu_delete -> {
                        showDeleteConfirmDialog(courseId, course.name) // 使用对象的属性
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun showEditCourseDialog(courseId: Int, course: Course) {
        val dialog = AddCourseDialog.newInstance(course)
        supportFragmentManager.setFragmentResultListener(AddCourseDialog.TAG, this) {
            _, bundle ->
                val updatedCourse = CompatibilityUtil.getParcelableCourse(bundle)
                if (updatedCourse != null) {
                    updateCourseInDb(courseId, updatedCourse)
                    loadCoursesFromDb()
                }
            }

        dialog.show(supportFragmentManager, "EditCourseDialog")
    }

    private fun Course.toContentValues(): ContentValues {
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

    private fun updateCourseInDb(
        id: Int, course: Course
    ) {
        val db = dbHelper.writableDatabase
        val rows = db.update(TABLE_NAME, course.toContentValues(), "id = ?", arrayOf(id.toString()))

        if (rows > 0) {
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show()
            loadCoursesFromDb()
        } else {
            Toast.makeText(this, "修改失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmDialog(courseId: Int, courseName: String) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("删除课程")
        builder.setMessage("确定要删除“$courseName”吗？此操作不可撤销。")

        builder.setPositiveButton("删除") { dialog, _ ->
            val db = dbHelper.writableDatabase
            val deletedRows = db.delete(
                CourseDbHelper.TABLE_NAME,
                "id = ?",
                arrayOf(courseId.toString())
            )
            if (deletedRows > 0) {
                Toast.makeText(this, "已成功删除课程：$courseName", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "已删除课程 $courseId")
                // 重新加载数据库并刷新列表，否则卡片还会留在屏幕上
                loadCoursesFromDb()
            } else {
                Toast.makeText(this, "删除失败，请重试", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "删除课程 $courseId 失败")
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss() // 直接关闭对话框，不执行任何操作
            Toast.makeText(this, "已取消删除课程：$courseName", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "取消删除课程 $courseId")
        }
        val alertDialog = builder.create()

        // 设置点击对话框外部不消失
        alertDialog.setCanceledOnTouchOutside(false)

        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
    }

    private fun setupAddCourseButton() {
        val dialog = AddCourseDialog.newInstance()
        supportFragmentManager.setFragmentResultListener(AddCourseDialog.TAG, this){
            _, bundle ->
            val course = CompatibilityUtil.getParcelableCourse(bundle)
            if (course != null) {
                saveCourseToDb(course)
            }
        }
        dialog.show(supportFragmentManager, "AddCourseDialog")
    }

    private fun saveCourseToDb(course: Course) {
        val db = dbHelper.writableDatabase
        val newRowId = db.insert(TABLE_NAME, null, course.toContentValues())
        if (newRowId != -1L) {
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show()
            loadCoursesFromDb()
        } else {
            Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show()
        }
    }
}