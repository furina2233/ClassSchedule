package com.lff.classschedule.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.pojo.Lesson
import com.lff.classschedule.receiver.LessonReminderReceiver
import com.lff.classschedule.util.CompatibilityUtil
import com.lff.classschedule.util.CourseTimeUtil
import com.lff.classschedule.util.ScreenUtil

class LessonInfoDialog : DialogFragment() {

    companion object {
        const val TAG = "LessonInfoDialog"
        fun newInstance(lesson: Lesson): DialogFragment {
            val args = Bundle()
            args.putParcelable("lesson", lesson)
            val fragment = LessonInfoDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var tvLessonName: TextView
    private lateinit var tvLessonTime: TextView
    private lateinit var tvLessonLocation: TextView

    private lateinit var lesson: Lesson

    private lateinit var btnClose: MaterialButton
    private lateinit var btnRemindMe: MaterialButton

    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_lesson_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置点击外部不关闭
        dialog?.setCanceledOnTouchOutside(false)

        tvLessonName = view.findViewById(R.id.tv_lesson_name)
        tvLessonTime = view.findViewById(R.id.tv_lesson_time)
        tvLessonLocation = view.findViewById(R.id.tv_lesson_location)

        if (arguments != null) {
            lesson = CompatibilityUtil.getParcelableLesson(requireArguments())!!
        }

        tvLessonName.text = lesson.name
        tvLessonTime.text = "${lesson.course.startWeek}-${lesson.course.endWeek}周  " +
                "${CourseTimeUtil.getDayOfWeekText(lesson.course.dayOfWeek)}  " +
                "  ${lesson.startLesson}-${lesson.endLesson}节  " +
                CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                    requireContext(),
                    lesson.startLesson,
                    lesson.endLesson
                )
        tvLessonLocation.text = lesson.course.location

        btnClose = view.findViewById(R.id.btn_close)
        btnClose.setOnClickListener {
            dismiss()
        }

        btnRemindMe = view.findViewById(R.id.btn_remind_me)
        btnRemindMe.setOnClickListener {
            val result = LessonReminderReceiver.setLessonReminder(requireContext(), lesson)
            Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show()
        }
    }
}