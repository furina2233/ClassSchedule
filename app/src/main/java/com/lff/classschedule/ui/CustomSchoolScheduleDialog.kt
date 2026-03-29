package com.lff.classschedule.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.util.ScreenUtil

class CustomSchoolScheduleDialog
    : DialogFragment() {

    companion object {
        const val TAG = "CustomSchoolScheduleDialog"
    }

    private var termCommencementTimeMonth: Int = 0
    private var termCommencementTimeDay: Int = 0
    private var maxWeeks: Int = 0
    private var maxLessonsPerDay: Int = 0
    private var durationPerLesson: Int = 0

    private lateinit var etMaxWeeks: EditText
    private lateinit var etMaxLessonsPerDay: EditText
    private lateinit var etDurationPerLesson: EditText

    private lateinit var etTermCommencementTimeMonth: EditText
    private lateinit var etTermCommencementTimeDay: EditText

    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_custom_school_schedule, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        termCommencementTimeMonth = SharedPreferenceConfig.getTermCommencementTimeMonth(context)
        termCommencementTimeDay = SharedPreferenceConfig.getTermCommencementTimeDay(context)
        maxWeeks = SharedPreferenceConfig.getMaxWeeksPerSemester(context)
        maxLessonsPerDay = SharedPreferenceConfig.getMaxLessonsPerDay(context)
        durationPerLesson = SharedPreferenceConfig.getDurationPerLesson(context)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etTermCommencementTimeMonth = view.findViewById(R.id.et_term_commencement_time_month)
        etTermCommencementTimeDay = view.findViewById(R.id.et_term_commencement_time_day)
        etMaxWeeks = view.findViewById(R.id.et_set_max_weeks)
        etMaxLessonsPerDay = view.findViewById(R.id.et_set_max_lessons_per_day)
        etDurationPerLesson = view.findViewById(R.id.et_set_duration_per_lesson)

        preFillData()

        // 设置点击外部不关闭
        dialog?.setCanceledOnTouchOutside(false)

        view.findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            performSave()
            if (maxWeeks != (etMaxWeeks.text.toString().toIntOrNull() ?: maxWeeks)) {
                showSyncConfirmDialog()
                Log.d(TAG, "修改了总周数")
            } else {
                Log.d(TAG, "没有修改总周数")
                dismiss()
            }
        }
    }

    // 将所有持续到超出当前总周数的课程同步
    private fun showSyncConfirmDialog() {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle("同步课程")
        builder.setMessage("是否将新的总周数应用到所有持续整个学期的课程？\n所有持续时长超出新总周数的课程的持续时长将会被截短。")

        builder.setPositiveButton("好的") { _, _ ->
            parentFragmentManager.setFragmentResult(TAG, Bundle().apply {
                putBoolean("need_sync", true)
                putInt("old_max_weeks", maxWeeks)
            })
            dismiss()
        }
        builder.setNegativeButton("取消") { _, _ ->
            parentFragmentManager.setFragmentResult(TAG, Bundle().apply {
                putBoolean("need_sync", false)
                putInt("old_max_weeks", maxWeeks)
            })
            dismiss()
        }

        val alertDialog = builder.create()

        // 设置点击对话框外部不消失
        alertDialog.setCanceledOnTouchOutside(false)

        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
    }

    private fun performSave() {
        val currentTermCommencementTimeMonth = etTermCommencementTimeMonth.text.toString().toIntOrNull()
        val currentTermCommencementTimeDay = etTermCommencementTimeDay.text.toString().toIntOrNull()
        val currentMaxWeeks = etMaxWeeks.text.toString().toIntOrNull()
        val currentMaxLessonsPerDay = etMaxLessonsPerDay.text.toString().toIntOrNull()
        val currentDurationPerLesson = etDurationPerLesson.text.toString().toIntOrNull()

        // 校验
        if (currentTermCommencementTimeMonth == null || currentTermCommencementTimeMonth !in 1..12 ||
            currentTermCommencementTimeDay == null || currentTermCommencementTimeDay !in 1..31
        ) {
            Toast.makeText(context, "请输入正确的学期开始时间", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val year = java.time.LocalDate.now().year
            java.time.LocalDate.of(year, currentTermCommencementTimeMonth, currentTermCommencementTimeDay)

        } catch (e: java.time.DateTimeException) {
            Toast.makeText(context, "请输入正确的学期开始时间", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentMaxWeeks == null || currentMaxWeeks <= 0) {
            Toast.makeText(context, "请输入正确的最大周数", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentMaxLessonsPerDay == null || currentMaxLessonsPerDay <= 0) {
            Toast.makeText(context, "请输入正确的每天最大课程数", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentDurationPerLesson == null || currentDurationPerLesson <= 0) {
            Toast.makeText(context, "请输入正确的课程时长", Toast.LENGTH_SHORT).show()
            return
        }

        SharedPreferenceConfig.apply {
            setTermCommencementTimeMonth(requireContext(), currentTermCommencementTimeMonth)
            setTermCommencementTimeDay(requireContext(), currentTermCommencementTimeDay)
            setMaxWeeksPerSemester(requireContext(), currentMaxWeeks)
            setMaxLessonsPerDay(requireContext(), currentMaxLessonsPerDay)
            setDurationPerLesson(requireContext(), currentDurationPerLesson)

            clearCache()
        }
        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
        Log.d(
            TAG,
            "保存新学期信息成功，最大周数：$currentMaxWeeks，每天最大课程数：$currentMaxLessonsPerDay，课程时长：$currentDurationPerLesson"
        )
    }

    private fun preFillData() {
        etTermCommencementTimeMonth.setText(termCommencementTimeMonth.toString())
        etTermCommencementTimeDay.setText(termCommencementTimeDay.toString())
        etMaxWeeks.setText(maxWeeks.toString())
        etMaxLessonsPerDay.setText(maxLessonsPerDay.toString())
        etDurationPerLesson.setText(durationPerLesson.toString())
    }
}