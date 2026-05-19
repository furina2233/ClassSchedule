package com.lff.classschedule

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lff.classschedule.database.AppDatabase
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.ui.AddCourseDialog
import com.lff.classschedule.ui.BatchAddCourseDialog
import com.lff.classschedule.ui.CourseCardAdapter
import com.lff.classschedule.util.CompatibilityUtil
import com.lff.classschedule.viewmodel.CoursesViewModel

class CoursesSettingActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CoursesSettingActivity"
    }

    private lateinit var viewModel: CoursesViewModel
    private lateinit var btnAddCourse: FloatingActionButton
    private lateinit var btnCourseSetting: ImageButton
    private lateinit var rvCourseList: RecyclerView
    private lateinit var adapter: CourseCardAdapter
    private lateinit var llBatchDeleteBar: LinearLayout
    private lateinit var tvNoCourses: TextView

    private var isBatchDeleteMode = false
    private val toDeleteCourseIds = mutableSetOf<Int>()
    private lateinit var batchDeleteBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courses_setting)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CoursesViewModel(AppDatabase.getInstance(this@CoursesSettingActivity).courseDao()) as T
            }
        })[CoursesViewModel::class.java]

        onBackPressedDispatcher.addCallback {
            Log.d(TAG, "已返回主界面")
            finish()
        }

        tvNoCourses = findViewById(R.id.tv_no_courses)
        rvCourseList = findViewById(R.id.rv_course_list)
        rvCourseList.layoutManager = LinearLayoutManager(this)

        adapter = CourseCardAdapter(
            onEdit = { courseId, course -> showEditCourseDialog(courseId, course) },
            onDelete = { courseId, courseName -> showDeleteConfirmDialog(courseId, courseName) },
            onBatchDeleteSelected = { courseId ->
                if (toDeleteCourseIds.contains(courseId)) {
                    toDeleteCourseIds.remove(courseId)
                } else {
                    toDeleteCourseIds.add(courseId)
                }
            },
            isBatchDeleteMode = { isBatchDeleteMode }
        )
        rvCourseList.adapter = adapter

        btnAddCourse = findViewById(R.id.btn_add_course)
        btnAddCourse.setOnClickListener {
            setupAddCourseButton()
        }

        btnCourseSetting = findViewById(R.id.btn_course_setting)
        btnCourseSetting.setOnClickListener {
            setupCourseSettingButton()
        }

        llBatchDeleteBar = findViewById(R.id.ll_batch_delete_bar)

        batchDeleteBackPressedCallback = onBackPressedDispatcher.addCallback(this, false) {
            exitBatchDeleteMode()
        }

        viewModel.courses.observe(this) { courses ->
            tvNoCourses.visibility = if (courses.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(courses.entries.map { it.toPair() })
        }

        viewModel.refreshCourses()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCourses()
    }

    private fun exitBatchDeleteMode() {
        isBatchDeleteMode = false
        toDeleteCourseIds.clear()

        llBatchDeleteBar.visibility = View.GONE
        btnCourseSetting.visibility = View.VISIBLE
        btnAddCourse.visibility = View.VISIBLE
        btnAddCourse.show()

        adapter.notifyDataSetChanged()
        batchDeleteBackPressedCallback.isEnabled = false

        Log.d(TAG, "已通过返回键取消批量删除模式")
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
        isBatchDeleteMode = true
        toDeleteCourseIds.clear()

        btnCourseSetting.visibility = View.INVISIBLE
        btnAddCourse.visibility = View.INVISIBLE
        batchDeleteBackPressedCallback.isEnabled = true

        val cbSelectAll = llBatchDeleteBar.findViewById<CheckBox>(R.id.cb_select_all)
        val btnConfirmDelete = llBatchDeleteBar.findViewById<Button>(R.id.btn_confirm_delete)

        adapter.notifyDataSetChanged()

        llBatchDeleteBar.visibility = View.VISIBLE

        cbSelectAll.setOnCheckedChangeListener(null)
        cbSelectAll.isChecked = false
        setupSelectAllListener(cbSelectAll)

        btnConfirmDelete.setOnClickListener {
            if (toDeleteCourseIds.isEmpty()) return@setOnClickListener

            val courses = viewModel.courses.value ?: return@setOnClickListener
            val toDeleteMap = toDeleteCourseIds.associateWith { id ->
                courses[id]?.name ?: ""
            }

            showBatchDeleteConfirmDialog(toDeleteMap)
        }
    }

    private fun setupSelectAllListener(cbSelectAll: CheckBox) {
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            toDeleteCourseIds.clear()
            if (isChecked) {
                val currentList = adapter.currentList
                for (entry in currentList) {
                    toDeleteCourseIds.add(entry.first)
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun showBatchDeleteConfirmDialog(toDeleteMap: Map<Int, String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("删除多门课程")

        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            val message = StringBuilder("确定删除以下课程吗？\n")
            toDeleteMap.values.forEach { message.append("\n· $it") }
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
            viewModel.deleteCourses(toDeleteMap.keys.toList())
            Toast.makeText(this, "成功删除 ${toDeleteMap.size} 门课程", Toast.LENGTH_SHORT).show()
            exitBatchDeleteMode()
            dialog.dismiss()
        }

        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
    }

    private fun batchAddCourse() {
        val dialog = BatchAddCourseDialog()
        dialog.show(supportFragmentManager, BatchAddCourseDialog.TAG)

        supportFragmentManager.setFragmentResultListener(BatchAddCourseDialog.TAG, this) { _, bundle ->
            val courseList = CompatibilityUtil.getParcelableCourseList(bundle)
            if (courseList != null) {
                viewModel.addCourses(courseList)
            }
        }
    }

    private fun showEditCourseDialog(courseId: Int, course: Course) {
        val dialog = AddCourseDialog.newInstance(course)
        supportFragmentManager.setFragmentResultListener(AddCourseDialog.TAG, this) { _, bundle ->
            val updatedCourse = CompatibilityUtil.getParcelableCourse(bundle)
            if (updatedCourse != null) {
                viewModel.updateCourse(courseId, updatedCourse)
            }
        }
        dialog.show(supportFragmentManager, AddCourseDialog.TAG)
    }

    private fun showDeleteConfirmDialog(courseId: Int, courseName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("删除课程")
        builder.setMessage("确定要删除\"$courseName\"吗？此操作不可撤销。")

        builder.setPositiveButton("删除") { dialog, _ ->
            viewModel.deleteCourse(courseId)
            Toast.makeText(this, "已成功删除课程：$courseName", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "已删除课程 $courseId")
            dialog.dismiss()
        }
        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
            Toast.makeText(this, "已取消删除课程：$courseName", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "取消删除课程 $courseId")
        }
        val alertDialog = builder.create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
    }

    private fun setupAddCourseButton() {
        val dialog = AddCourseDialog.newInstance()
        supportFragmentManager.setFragmentResultListener(AddCourseDialog.TAG, this) { _, bundle ->
            val course = CompatibilityUtil.getParcelableCourse(bundle)
            if (course != null) {
                viewModel.addCourse(course)
            }
        }
        dialog.show(supportFragmentManager, AddCourseDialog.TAG)
    }
}
