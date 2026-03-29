package com.lff.classschedule.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lff.classschedule.R
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.util.CompatibilityUtil
import com.lff.classschedule.util.CourseTimeUtil
import com.lff.classschedule.util.ScreenUtil

class LessonInfoDialog : DialogFragment() {

    companion object {
        const val TAG = "LessonInfoDialog"
        fun newInstance(course: Course): DialogFragment {
            val args = Bundle()
            args.putParcelable("course", course)
            val fragment = LessonInfoDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var tvLessonName: TextView
    private lateinit var tvLessonTime: TextView
    private lateinit var tvLessonLocation: TextView

    private lateinit var course: Course

    private lateinit var btnClose: MaterialButton

    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_lesson_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvLessonName = view.findViewById(R.id.tv_lesson_name)
        tvLessonTime = view.findViewById(R.id.tv_lesson_time)
        tvLessonLocation = view.findViewById(R.id.tv_lesson_location)

        if (arguments != null) {
            course = CompatibilityUtil.getParcelableCourse(requireArguments())!!
        }

        tvLessonName.text = course.name
        tvLessonTime.text = "${course.startWeek}-${course.endWeek}周  " +
                "${CourseTimeUtil.getDayOfWeekText(course.dayOfWeek)}  " +
                "  ${course.startLesson}-${course.endLesson}节  " +
                "${
                    CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                        requireContext(),
                        course.startLesson,
                        course.endLesson
                    )
                }"
        tvLessonLocation.text = course.location

        btnClose = view.findViewById(R.id.btn_close)
        btnClose.setOnClickListener {
            dismiss()
        }
    }
}