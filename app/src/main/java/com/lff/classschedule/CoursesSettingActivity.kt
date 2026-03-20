package com.lff.classschedule

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lff.classschedule.database.CourseDbHelper
import com.lff.classschedule.database.CourseDbHelper.Companion.TABLE_NAME

class CoursesSettingActivity : AppCompatActivity() {

    val TAG = "CoursesSettingActivity"

    private lateinit var dbHelper: CourseDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courses_setting)

        dbHelper = CourseDbHelper(this)
        val db = dbHelper.writableDatabase
        if (db.isOpen) {
            Log.d(TAG, "数据库已就绪，表 $TABLE_NAME 已检查/创建")
        }

        onBackPressedDispatcher.addCallback{
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
    }

    private fun loadCoursesFromDb() {
        val container = findViewById<LinearLayout>(R.id.ll_course_list)
        container.removeAllViews() // 先清空，防止重复添加

        val db = dbHelper.readableDatabase
        // 查询所有课程
        val cursor = db.query(
            CourseDbHelper.TABLE_NAME,
            null, null, null, null, null,
            "${CourseDbHelper.COL_DAY_OF_WEEK} ASC" // 按星期几排序
        )

        if (cursor.moveToFirst()) {
            Log.d(TAG,"数据库中有课程")
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_NAME))
                val startWeek = cursor.getInt(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_START_WEEK))
                val endWeek = cursor.getInt(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_END_WEEK))
                val dayOfWeek = cursor.getInt(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_DAY_OF_WEEK))
                val timeRange = cursor.getString(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_COURSE_TIME))
                val location = cursor.getString(cursor.getColumnIndexOrThrow(CourseDbHelper.COL_COURSE_LOCATION))

                // 加载卡片布局
                val itemView = LayoutInflater.from(this).inflate(R.layout.item_course_card, container, false)

                val btnMore = itemView.findViewById<ImageButton>(R.id.btn_more)
                setupPopupMenu(btnMore, id, name)

                // 填充数据到卡片组件
                itemView.findViewById<TextView>(R.id.tv_course_name).text = name

                // 格式化周数：1-18 周
                val weekText = "$startWeek-$endWeek 周"
                itemView.findViewById<TextView>(R.id.tv_course_weeks).text = weekText

                // 格式化时间：周一 9:50-12:15
                val timeText = "${getDayOfWeekText(dayOfWeek)} $timeRange"
                itemView.findViewById<TextView>(R.id.tv_course_time).text = timeText

                itemView.findViewById<TextView>(R.id.tv_course_location).text = location


                // 将卡片放入滚动视图的容器中
                container.addView(itemView)

            } while (cursor.moveToNext())
        }else{
            Log.d(TAG, "数据库中无课程")
            findViewById<TextView>(R.id.tv_no_courses).visibility = View.VISIBLE
        }
        cursor.close()
        Log.d(TAG, "已从数据库加载课程数据")
    }

    fun setupPopupMenu(btnMore: ImageButton, courseId : Int, courseName: String){
        btnMore.setOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menuInflater.inflate(R.menu.course_item_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_edit -> {
                        // TODO: 跳转到编辑页
                        true
                    }
                    R.id.menu_delete -> {
                        showDeleteConfirmDialog(courseId,courseName)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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
                Toast.makeText(this, "已成功删除课程$courseName", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "已取消删除课程$courseName", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "取消删除课程 $courseId")
        }
        val alertDialog = builder.create()

        // 设置点击对话框外部不消失
        alertDialog.setCanceledOnTouchOutside(false)

        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
    }

    private fun getDayOfWeekText(day: Int): String {
        return when (day) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> "未知"
        }
    }
}