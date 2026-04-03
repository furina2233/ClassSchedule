package com.lff.classschedule

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
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.database.CourseDbHelper
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.ui.AddCourseDialog
import com.lff.classschedule.ui.BatchAddCourseDialog
import com.lff.classschedule.ui.CustomSchoolScheduleDialog
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

        // 从课程设置页面返回时一定返回主页
        onBackPressedDispatcher.addCallback {
            Log.d(TAG, "已返回主界面")
            finish()
        }

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

        // 首次使用时
        if (SharedPreferenceConfig.getBoolean(this, SharedPreferenceConfig.KEY_IS_FIRST_ADD_COURSE)) {
            SharedPreferenceConfig.setBoolean(this, SharedPreferenceConfig.KEY_IS_FIRST_ADD_COURSE, false)
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }
    }

    private fun exitBatchDeleteMode() {
        llBatchDeleteBar.visibility = View.GONE
        btnCourseSetting.visibility = View.VISIBLE
        btnAddCourse.visibility = View.VISIBLE

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
            val needSync = bundle.getBoolean("need_sync")
            val newMaxWeeks = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_MAX_WEEKS)
            val oldMaxWeeks = bundle.getInt("old_max_weeks")

            if (newMaxWeeks > oldMaxWeeks) {
                // 学期变长
                if (needSync) {
                    val rows = dbHelper.syncCoursesWhenSemesterLengthened(oldMaxWeeks, newMaxWeeks)
                    if (rows > 0) {
                        Toast.makeText(this, "已同步课程的持续时长", Toast.LENGTH_SHORT).show()
                    }
                    Log.d(TAG, "学期变长：已将 $rows 门全学期课程更新至 $newMaxWeeks 周")
                }
            } else {
                // 学期变短
                val rows = dbHelper.syncCoursesWhenSemesterShortened(newMaxWeeks)
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

                else -> false
            }
        }
        popup.show()
    }

    private fun batchDeleteCourse() {
        btnCourseSetting.visibility = View.INVISIBLE
        btnAddCourse.visibility = View.INVISIBLE
        batchDeleteBackPressedCallback.isEnabled = true

        val toDeleteCourseList = mutableSetOf<String>()

        val cbSelectAll = llBatchDeleteBar.findViewById<CheckBox>(R.id.cb_select_all)
        val btnConfirmDelete = llBatchDeleteBar.findViewById<Button>(R.id.btn_confirm_delete)

        for (item in llCourseList.children) {
            val btnDelete = item.findViewById<ImageButton>(R.id.btn_delete).apply {
                visibility = View.VISIBLE
            }
            val clCard = item.findViewById<ConstraintLayout>(R.id.cl_card)

            btnDelete.setOnClickListener {
                val isSelected = it.tag as? Boolean ?: false
                if (!isSelected) {
                    clCard.setBackgroundResource(R.drawable.bg_to_delete_course_card)
                    toDeleteCourseList.add(clCard.tag.toString())
                    it.tag = true
                } else {
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
            val result = dbHelper.deleteCourseByIds(toDeleteCoursesMap.keys.toList())
            if (result) {
                Log.d(TAG, "已删除 ${toDeleteCoursesMap.size} 门课程")
                Toast.makeText(this, "成功删除 ${toDeleteCoursesMap.size} 门课程", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "删除失败")
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
            }

            llBatchDeleteBar.visibility = View.GONE
            btnCourseSetting.visibility = View.VISIBLE
            loadCoursesFromDb()
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
        dialog.show(supportFragmentManager, BatchAddCourseDialog.TAG)

        supportFragmentManager.setFragmentResultListener(BatchAddCourseDialog.TAG, this) { _, bundle ->
            val courseList = CompatibilityUtil.getParcelableCourseList(bundle)
            if (courseList != null) {
                for (course in courseList) {
                    dbHelper.addCourse(course)
                    Log.d(TAG, "已添加课程：$course")
                }
                loadCoursesFromDb()
            }
        }
    }


    private fun loadCoursesFromDb() {
        val container = findViewById<LinearLayout>(R.id.ll_course_list)
        container.removeAllViews()  // 先清空，防止重复添加
        findViewById<TextView>(R.id.tv_no_courses).visibility = View.GONE

        val courses = dbHelper.queryAllCourses().also {
            if (it.isEmpty()) {
                findViewById<TextView>(R.id.tv_no_courses).visibility = View.VISIBLE
            }
        }

        for (entry in courses) {
            // 加载卡片布局
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_course_card, container, false)

            itemView.findViewById<ConstraintLayout>(R.id.cl_card).tag = entry.key.toString()  // 把卡片绑定数据库id

            val btnMore = itemView.findViewById<ImageButton>(R.id.btn_more)
            setupPopupMenu(btnMore, entry.key, entry.value)

            itemView.findViewById<TextView>(R.id.tv_course_name).text = entry.value.name

            // 格式化周数：1-18 周
            val weekText = "${entry.value.startWeek}-${entry.value.endWeek} 周"
            itemView.findViewById<TextView>(R.id.tv_course_weeks).text = weekText

            // 格式化时间：周一 9:50-12:15
            val timeText = CourseTimeUtil.getDayOfWeekText(entry.value.dayOfWeek) + "  " +
                    CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                        this, entry.value.startLesson, entry.value.endLesson
                    )
            itemView.findViewById<TextView>(R.id.tv_course_time).text = timeText

            itemView.findViewById<TextView>(R.id.tv_course_location).text = entry.value.location

            // 将卡片放入滚动视图的容器中
            container.addView(itemView)
        }

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
                dbHelper.updateCourse(courseId, updatedCourse)
                loadCoursesFromDb()
            }
        }

        dialog.show(supportFragmentManager, AddCourseDialog.TAG)
    }

    private fun showDeleteConfirmDialog(courseId: Int, courseName: String) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("删除课程")
        builder.setMessage("确定要删除“$courseName”吗？此操作不可撤销。")

        builder.setPositiveButton("删除") { dialog, _ ->
            if (dbHelper.deleteCourseById(courseId)) {
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
                dbHelper.addCourse(course)
                loadCoursesFromDb()
            }
        }
        dialog.show(supportFragmentManager, AddCourseDialog.TAG)
    }
}