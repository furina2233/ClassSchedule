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
            // 1. 校验必填字段是否存在且类型正确 (has() 检查键，opt... 检查类型)
            val hasFields = json.has("name") && json.has("startWeek") &&
                    json.has("endWeek") && json.has("dayOfWeek") &&
                    json.has("startLesson") && json.has("endLesson") &&
                    json.has("location")

            // 2. 进一步检查类型（利用 opt 系列，如果类型不对会返回默认值）
            val typeCheck = json.optString("name").isNotEmpty() &&
                    json.opt("startWeek") is Int &&
                    json.opt("endWeek") is Int &&
                    json.opt("dayOfWeek") is Int &&
                    json.opt("startLesson") is Int &&
                    json.opt("endLesson") is Int &&
                    json.optString("location") is String

            // 3. 业务逻辑校验（可选：比如开始周不能大于结束周）
            val businessCheck = json.getInt("startWeek") <= json.getInt("endWeek") &&
                    json.getInt("startLesson") <= json.getInt("endLesson")

            hasFields && typeCheck && businessCheck
        } catch (e: Exception) {
            false
        }
    }
}