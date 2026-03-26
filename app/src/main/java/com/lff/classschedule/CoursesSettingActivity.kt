package com.lff.classschedule

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lff.classschedule.config.SchoolScheduleConfig
import com.lff.classschedule.database.CourseDbHelper
import com.lff.classschedule.database.CourseDbHelper.Companion.TABLE_NAME
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.ui.AddCourseDialog
import com.lff.classschedule.ui.BatchAddCourseDialog
import com.lff.classschedule.ui.CustomSchoolScheduleDialog
import com.lff.classschedule.ui.SetStartTimesDialog
import com.lff.classschedule.util.CompatibilityUtil
import com.lff.classschedule.util.CourseTimeUtil

class CoursesSettingActivity : AppCompatActivity() {

    val TAG = "CoursesSettingActivity"

    private lateinit var dbHelper: CourseDbHelper

    private lateinit var btnAddCourse: FloatingActionButton
    private lateinit var btnCourseSetting: ImageButton

    private lateinit var llCourseList: LinearLayout
    private lateinit var llBatchDeleteBar: LinearLayout

    private lateinit var batchDeleteBackPressedCallback: OnBackPressedCallback

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

        llCourseList = findViewById(R.id.ll_course_list)
        llBatchDeleteBar = findViewById(R.id.ll_batch_delete_bar)

        batchDeleteBackPressedCallback = onBackPressedDispatcher.addCallback(this, false) {
            exitBatchDeleteMode()
        }
    }

    private fun exitBatchDeleteMode() {
        llBatchDeleteBar.visibility = View.GONE
        btnCourseSetting.visibility = View.VISIBLE

        btnAddCourse.show()

        for (item in llCourseList.children) {
            item.findViewById<ImageButton>(R.id.btn_delete).visibility = View.GONE
            item.findViewById<ConstraintLayout>(R.id.cl_card).setBackgroundResource(R.drawable.bg_course_card)
            item.findViewById<ImageButton>(R.id.btn_delete).tag = false
        }

        batchDeleteBackPressedCallback.isEnabled = false

        Log.d(TAG, "已通过返回键取消批量删除模式")
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
                    batchAddCourse()
                    true
                }

                R.id.menu_batch_delete -> {
                    batchDeleteCourse()
                    true
                }

                R.id.menu_custom_school_schedule -> {
                    setCustomSchoolSchedule()
                    true
                }

                R.id.menu_set_start_times -> {
                    setStartTimes()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun batchDeleteCourse() {
        btnCourseSetting.visibility = View.INVISIBLE
        batchDeleteBackPressedCallback.isEnabled = true

        val toDeleteCourseList = mutableSetOf<String>()

        val cbSelectAll = llBatchDeleteBar.findViewById<CheckBox>(R.id.cb_select_all)
        val btnConfirmDelete = llBatchDeleteBar.findViewById<Button>(R.id.btn_confirm_delete)

        for (item in llCourseList.children){
            val btnDelete = item.findViewById<ImageButton>(R.id.btn_delete).apply {
                visibility = View.VISIBLE
            }
            val clCard = item.findViewById<ConstraintLayout>(R.id.cl_card)

            btnDelete.setOnClickListener {
                val isSelected = it.tag as? Boolean ?: false
                if (!isSelected){
                    clCard.setBackgroundResource(R.drawable.bg_to_delete_course_card)
                    toDeleteCourseList.add(clCard.tag.toString())
                    it.tag = true
                }else{
                    clCard.setBackgroundResource(R.drawable.bg_course_card)
                    toDeleteCourseList.remove(clCard.tag.toString())
                    it.tag = false
                }
                cbSelectAll.setOnCheckedChangeListener(null)
                cbSelectAll.isChecked = toDeleteCourseList.size == llCourseList.childCount
                setupSelectAllListener(cbSelectAll)
            }
        }

        llBatchDeleteBar.visibility = View.VISIBLE

        setupSelectAllListener(cbSelectAll)

        btnConfirmDelete.setOnClickListener {
            val toDeleteCoursesMap = mutableMapOf<Int, String>()

            // 记录要删除的课程的名字
            for (item in llCourseList.children) {
                val btnDelete = item.findViewById<ImageButton>(R.id.btn_delete)
                val isSelected = btnDelete.tag as? Boolean ?: false

                if (isSelected) {
                    val courseName = item.findViewById<TextView>(R.id.tv_course_name).text.toString()
                    val courseId = item.findViewById<ConstraintLayout>(R.id.cl_card).tag.toString()
                    toDeleteCoursesMap[courseId.toInt()] = courseName
                }
            }

            if (toDeleteCoursesMap.isEmpty()) return@setOnClickListener

            showBatchDeleteConfirmDialog(toDeleteCoursesMap)
        }
    }

    private fun showBatchDeleteConfirmDialog(toDeleteCoursesMap: Map<Int, String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("删除多门课程")

        // 用滚动视图展示，防止要删除的课程过多导致挤不下
        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            val message = StringBuilder("确定删除以下课程吗？\n")
            toDeleteCoursesMap.values.forEach { message.append("\n· $it") }
            text = message.toString()
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(0f, 1.2f)

            setPadding(60, 40, 60, 0)
        }
        scrollView.addView(textView)

        val maxHeight = (resources.displayMetrics.heightPixels * 0.5).toInt()
        scrollView.post {
            if (scrollView.measuredHeight > maxHeight) {
                val params = scrollView.layoutParams
                params.height = maxHeight
                scrollView.layoutParams = params
            }
        }

        builder.setView(scrollView)

        builder.setPositiveButton("确定删除") { dialog, _ ->
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                for (courseId in toDeleteCoursesMap.keys) {
                    if (deleteCourseFromDb(courseId)) {
                        Log.d(TAG, "已删除：${toDeleteCoursesMap[courseId]} (ID:$courseId)")
                    }
                }
                db.setTransactionSuccessful()
                Toast.makeText(this, "成功删除 ${toDeleteCoursesMap.size} 门课程", Toast.LENGTH_SHORT).show()
            } finally {
                db.endTransaction()
                llBatchDeleteBar.visibility = View.GONE
                btnCourseSetting.visibility = View.VISIBLE
                loadCoursesFromDb()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
    }

    private fun setupSelectAllListener(cbSelectAll: CheckBox) {
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            for (item in llCourseList.children) {
                val btnDelete = item.findViewById<ImageButton>(R.id.btn_delete)
                val isSelected = btnDelete.tag as? Boolean ?: false

                // 如果当前的选中状态和全选框的选中状态不一致，则触发点击事件
                if (isSelected != isChecked) {
                    btnDelete.performClick()
                }
            }
        }
    }

    private fun batchAddCourse() {
        //TODO:升级为在Activity中进行操作
        val dialog = BatchAddCourseDialog()
        dialog.show(supportFragmentManager, "BatchAddCourseDialog")

        supportFragmentManager.setFragmentResultListener(BatchAddCourseDialog.TAG, this) { _, bundle ->
            val courseList = CompatibilityUtil.getParcelableCourseList(bundle)
            if (courseList != null) {
                for (course in courseList) {
                    saveCourseToDb(course)
                    Log.d(TAG, "已添加课程：$course")
                }
            }
        }
        loadCoursesFromDb()
    }

    private fun setStartTimes() {
        val dialog = SetStartTimesDialog()
        dialog.show(supportFragmentManager, "SetStartTimesDialog")
        supportFragmentManager.setFragmentResultListener(SetStartTimesDialog.TAG, this) { _, bundle ->
            val newStartTimes = bundle.getStringArray("new_start_times")
            newStartTimes?.let { SchoolScheduleConfig.setStartTimes(this, it) }
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "已保存新的课程开始时间：${newStartTimes?.joinToString(",")}")
        }
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

                itemView.findViewById<ConstraintLayout>(R.id.cl_card).tag = id.toString()  // 把卡片绑定数据库id

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
                        showEditCourseDialog(courseId, course)
                        true
                    }

                    R.id.menu_delete -> {
                        showDeleteConfirmDialog(courseId, course.name)
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
        supportFragmentManager.setFragmentResultListener(AddCourseDialog.TAG, this) { _, bundle ->
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
            if (deleteCourseFromDb(courseId)) {
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
        supportFragmentManager.setFragmentResultListener(AddCourseDialog.TAG, this) { _, bundle ->
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

    private fun deleteCourseFromDb(courseId: Int): Boolean {
        val db = dbHelper.writableDatabase
        return try {
            val deletedRows = db.delete(
                CourseDbHelper.TABLE_NAME,
                "id = ?",
                arrayOf(courseId.toString())
            )
            deletedRows > 0
        } catch (e: Exception) {
            Log.e(TAG, "删除课程 $courseId 时发生异常: ${e.message}")
            false
        }
    }
}