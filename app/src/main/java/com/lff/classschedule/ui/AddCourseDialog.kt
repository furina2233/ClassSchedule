package com.lff.classschedule.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.util.CompatibilityUtil
import com.lff.classschedule.util.CourseTimeUtil
import com.lff.classschedule.util.ScreenUtil

// TODO:把AddCourseDialog的构造函数改为无参的，否则翻转手机时会出现RuntimeException
class AddCourseDialog : DialogFragment() {

    companion object {
        const val TAG = "AddCourseDialog"

        fun newInstance(): AddCourseDialog {
            return AddCourseDialog()
        }

        fun newInstance(course: Course? = null): AddCourseDialog {
            val fragment = AddCourseDialog()
            course?.let {
                fragment.arguments = Bundle().apply {
                    putParcelable("course", it)
                }
            }
            return fragment
        }
    }

    private lateinit var tvTitle: TextView
    private lateinit var etName: EditText
    private lateinit var etLocation: EditText
    private lateinit var spStartWeek: Spinner
    private lateinit var spEndWeek: Spinner
    private lateinit var spDay: Spinner
    private lateinit var spStartLesson: Spinner
    private lateinit var spEndLesson: Spinner
    private lateinit var btnSave: Button

    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_add_course, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置点击外部不关闭
        dialog?.setCanceledOnTouchOutside(false)

        tvTitle = view.findViewById(R.id.tv_add_course_dialog_title)
        etName = view.findViewById(R.id.et_course_name)
        etLocation = view.findViewById(R.id.et_location)
        spStartWeek = view.findViewById(R.id.spinner_start_week)
        spEndWeek = view.findViewById(R.id.spinner_end_week)
        spDay = view.findViewById(R.id.spinner_day_of_week)
        spStartLesson = view.findViewById(R.id.spinner_start_lesson)
        spEndLesson = view.findViewById(R.id.spinner_end_lesson)
        btnSave = view.findViewById(R.id.btn_save)

        // 设置点击外部不关闭
        dialog?.setCanceledOnTouchOutside(false)

        val maxWeeks = SharedPreferenceConfig.getInt(requireContext(), SharedPreferenceConfig.KEY_MAX_WEEKS)
        val weeks = (1..maxWeeks).map { "第 $it 周" }
        // 开始周：1-18 正序
        spStartWeek.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            weeks
        )
        // 结束周：18-1 倒序
        spEndWeek.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            weeks.reversed()
        )

        val days = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        spDay.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, days)

        val maxLessons = SharedPreferenceConfig.getString(requireContext(), SharedPreferenceConfig.KEY_START_TIMES).split(',').size
        val lessons = (1..maxLessons).map { "第 $it 节" }
        val lessonAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, lessons)
        spStartLesson.adapter = lessonAdapter
        spEndLesson.adapter = lessonAdapter

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val location = etLocation.text.toString().trim()

            // 获取选择的数值
            val startWeek = spStartWeek.selectedItem.toString().filter { it.isDigit() }.toInt()
            val endWeek = spEndWeek.selectedItem.toString().filter { it.isDigit() }.toInt()
            val dayOfWeek = spDay.selectedItemPosition + 1 // 数据库存储 1-7
            val startLessonIdx = spStartLesson.selectedItemPosition
            val endLessonIdx = spEndLesson.selectedItemPosition

            // 校验数据
            if (name.isEmpty()) {
                Toast.makeText(context, "请输入课程名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (startWeek > endWeek) {
                Toast.makeText(context, "起始周不能大于结束周", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (startLessonIdx > endLessonIdx) {
                Toast.makeText(context, "起始节次不能晚于结束节次", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timeRange = CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                requireContext(), startLessonIdx + 1, endLessonIdx + 1
            )
            if (timeRange == "") {
                Toast.makeText(context, "请检查课程时间设置", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 回调给 Activity 执行数据库插入
            val course = Course(name, startWeek, endWeek, dayOfWeek, startLessonIdx + 1, endLessonIdx + 1, location)
            parentFragmentManager.setFragmentResult(TAG, Bundle().apply {
                putParcelable("course", course)
            })
            dismiss()
        }

        preFillData()  // 填充数据，如果是新增模式，则自动返回

        Log.d(TAG, "添加课程窗口加载完成")
    }

    // 如果是编辑模式，则填充数据
    private fun preFillData() {
        val course = arguments?.let { CompatibilityUtil.getParcelableCourse(it) } ?: return

        val totalWeeks = SharedPreferenceConfig.getInt(requireContext(), SharedPreferenceConfig.KEY_MAX_WEEKS) // 获取当前设定的总周数

        tvTitle.text = "修改课程"
        etName.setText(course.name)
        etLocation.setText(course.location)

        spStartWeek.setSelection(course.startWeek - 1)
        spEndWeek.setSelection(totalWeeks - course.endWeek)

        spDay.setSelection(course.dayOfWeek - 1)
        spStartLesson.setSelection(course.startLesson - 1)
        spEndLesson.setSelection(course.endLesson - 1)

        Log.d(TAG, "修改课程模式下，已填充课程数据")
    }
}