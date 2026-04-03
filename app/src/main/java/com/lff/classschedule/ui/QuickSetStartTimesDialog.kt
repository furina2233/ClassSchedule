package com.lff.classschedule.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.util.ScreenUtil

@Deprecated("现在在设置页面中进行相关设置")
class QuickSetStartTimesDialog : DialogFragment() {

    companion object {
        const val TAG = "QuickSetStartTimesDialog"
    }

    private lateinit var tvCurrentLessons: TextView
    private lateinit var etStartTimes: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private lateinit var startTimes: Array<String>

    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_quick_set_start_times, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        startTimes =
            SharedPreferenceConfig.getString(requireContext(), SharedPreferenceConfig.KEY_START_TIMES).split(",")
                .toTypedArray()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置点击外部不关闭
        dialog?.setCanceledOnTouchOutside(false)

        tvCurrentLessons = view.findViewById(R.id.tv_current_lessons)
        tvCurrentLessons.text = startTimes.size.toString()


        etStartTimes = view.findViewById(R.id.et_start_times)
        etStartTimes.setText(startTimes.joinToString("\n"))
        etStartTimes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                val count = text.split("\n").size
                tvCurrentLessons.text = count.toString()
            }
        })

        btnSave = view.findViewById(R.id.btn_save)
        btnSave.setOnClickListener {
            val startTimes = etStartTimes.text.toString().split("\n").toTypedArray().map {
                it.trim().replace("：", ":")
            }.filter {
                it.isNotEmpty()
            }.toTypedArray()
            try {
                for (time in startTimes) {
                    val timeArray = time.split(":")
                    val h = timeArray[0].toInt()
                    val m = timeArray[1].toInt()
                    if (h !in 0..23 || m !in 0..59) {
                        throw Exception()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "请输入正确的内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performSave(startTimes)
        }
    }

    private fun performSave(startTimes: Array<String>) {
        val text = startTimes.joinToString(",")
        parentFragmentManager.setFragmentResult(TAG, Bundle().apply {
            putString("start_times", text)
        })
        Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}