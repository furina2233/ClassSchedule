package com.lff.classschedule.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lff.classschedule.R
import com.lff.classschedule.util.ScreenUtil

class HowToBatchAddCourseDialog : DialogFragment() {
    companion object {
        const val TAG = "HowToBatchAddCourseDialog"
    }

    private lateinit var btnIGotIt: MaterialButton
    private lateinit var btnClickToCopy: MaterialButton

    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_how_to_batch_add_course, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置点击外部不关闭
        dialog?.setCanceledOnTouchOutside(false)

        btnIGotIt = view.findViewById(R.id.btn_i_got_it)
        btnIGotIt.setOnClickListener {
            dismiss()
        }

        btnClickToCopy = view.findViewById(R.id.btn_click_to_copy)
        btnClickToCopy.setOnClickListener {
            onBtnClickToCopyClicked()
        }
    }

    private fun onBtnClickToCopyClicked() {
        try {
            val textToCopy = getString(R.string.prompt)
            if (textToCopy.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("prompt", textToCopy)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(requireContext(), "复制成功", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "复制失败", Toast.LENGTH_SHORT).show()
        }
    }
}