package com.lff.classschedule.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.util.ScreenUtil
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BatchAddCourseDialog : DialogFragment() {
    companion object {
        const val TAG = "BatchAddCourseDialog"
    }

    private lateinit var etBatchInput: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnHowToBatchAddCourse: MaterialButton

    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_batch_add_course, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置点击外部不关闭
        dialog?.setCanceledOnTouchOutside(false)

        etBatchInput = view.findViewById(R.id.et_batch_input)
        btnSave = view.findViewById(R.id.btn_save)

        btnSave.setOnClickListener {
            onBtnSaveClicked()
        }

        btnHowToBatchAddCourse = view.findViewById(R.id.btn_how_to_batch_add_course)

        btnHowToBatchAddCourse.setOnClickListener {
            val dialog = HowToBatchAddCourseDialog()
            dialog.show(parentFragmentManager, HowToBatchAddCourseDialog.TAG)
        }

    }

    private fun onBtnSaveClicked() {
        val inputText = etBatchInput.text.toString()
        try {
            val jsonArray = JSONArray(inputText)

            val validCourses = ArrayList<JSONObject>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                if (isCourseValid(item)) {
                    validCourses.add(item)
                } else {
                    throw JSONException("第 ${i + 1} 个课程数据格式不正确，请检查字段名称和类型")
                }
            }

            val courseList = ArrayList<Course>()
            for (item in validCourses) {
                courseList.add(
                    Course(
                        item.getString("name"),
                        item.getInt("startWeek"),
                        item.getInt("endWeek"),
                        item.getInt("dayOfWeek"),
                        item.getInt("startLesson"),
                        item.getInt("endLesson"),
                        item.getString("location")
                    ).also {
                        Log.d(TAG, "将要添加的课程：$it")
                    }
                )
            }

            Toast.makeText(context, "成功添加 ${courseList.size} 门课程", Toast.LENGTH_LONG).show()

            parentFragmentManager.setFragmentResult(TAG, Bundle().apply {
                putParcelableArrayList("course_list", courseList)
            })
            dismiss()
        } catch (e: JSONException) {
            val errorMessage = if (e.message!!.startsWith("第")) {
                e.message!!
            } else {
                "输入的JSON格式错误"
            }
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            return
        }
    }

    private fun isCourseValid(json: JSONObject): Boolean {
        return try {
            // 校验字段是否存在
            val hasFields = json.has("name") && json.has("startWeek") &&
                    json.has("endWeek") && json.has("dayOfWeek") &&
                    json.has("startLesson") && json.has("endLesson") &&
                    json.has("location")

            // 校验字段类型
            val typeCheck = json.optString("name").isNotEmpty() &&
                    json.opt("startWeek") is Int &&
                    json.opt("endWeek") is Int &&
                    json.opt("dayOfWeek") is Int &&
                    json.opt("startLesson") is Int &&
                    json.opt("endLesson") is Int &&
                    json.optString("location") is String

            // 逻辑校验：开始周要小于结束周，开始节数要小于结束节数，结束节数要小于等于最大节数
            val maxLesson = SharedPreferenceConfig.getString(requireContext(), SharedPreferenceConfig.KEY_START_TIMES)
                .split(",").size
            val logicCheck = json.getInt("startWeek") <= json.getInt("endWeek") &&
                    json.getInt("startLesson") <= json.getInt("endLesson") &&
                    json.getInt("endLesson") <= maxLesson

            hasFields && typeCheck && logicCheck
        } catch (e: Exception) {
            false
        }
    }
}