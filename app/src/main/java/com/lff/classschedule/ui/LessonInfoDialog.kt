package com.lff.classschedule.ui

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.pojo.Lesson
import com.lff.classschedule.receiver.LessonReminderReceiver
import com.lff.classschedule.util.CompatibilityUtil
import com.lff.classschedule.util.CourseTimeUtil
import com.lff.classschedule.util.PermissionUtil
import com.lff.classschedule.util.ScreenUtil
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

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
    private lateinit var reminderDescription: String
    private var reminderTime: Long = -1

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.entries.all { it.value }
        if (isGranted) {
            setCalendarSchedule()
        } else {
            // 如果被用户彻底拒绝，弹出跳转框
            if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR)) {
                PermissionUtil.goToCalendarSettings(requireActivity(), "需要开启日历读写权限，否则无法提醒你。")
            } else {
                Toast.makeText(requireContext(), "添加日历失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            lesson = CompatibilityUtil.getParcelableLesson(requireArguments())!!
            reminderDescription = "${
                CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                    requireContext(),
                    lesson.startLesson,
                    lesson.endLesson
                )
            } " +
                    "在 ${lesson.course.location} 上 ${lesson.name} 课。"
        }

        tvLessonName.text = lesson.name
        tvLessonTime.text = CourseTimeUtil.getFormatLessonTime(requireContext(), lesson)
        tvLessonLocation.text = lesson.course.location

        btnClose = view.findViewById(R.id.btn_close)
        btnClose.setOnClickListener {
            dismiss()
        }

        btnRemindMe = view.findViewById(R.id.btn_remind_me)
        btnRemindMe.setOnClickListener {
            onRemindMeButtonClick()
        }

        reminderTime = lesson.startTime.minusMinutes(
            SharedPreferenceConfig.getInt(
                requireContext(),
                SharedPreferenceConfig.KEY_REMINDER_TIME
            ).toLong()
        ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun onRemindMeButtonClick() {
        val context = requireContext()
        val remindWay = SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_REMIND_WAY)

        val latestReminderTime = System.currentTimeMillis() -
                SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_REMINDER_TIME) * 60000L

        if (reminderTime < latestReminderTime) {
            Toast.makeText(context, "这节课已经上过啦！", Toast.LENGTH_SHORT).show()
            return
        }

        when (remindWay) {
            SharedPreferenceConfig.RemindWay.WAY_CALENDAR_SCHEDULE -> {
                if (PermissionUtil.hasCalendarPermission(context)) {
                    setCalendarSchedule()
                } else {
                    calendarPermissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                    )
                }
            }

            SharedPreferenceConfig.RemindWay.WAY_ALARM -> {
                setAlarm()
            }

            SharedPreferenceConfig.RemindWay.WAY_NOTIFICATION -> {
                LessonReminderReceiver.setLessonReminder(context, lesson, reminderTime)
            }
        }
    }

    private fun setAlarm() {
        // 闹钟提前指定分钟响起
        val alarmTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(reminderTime), ZoneId.systemDefault())
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, reminderDescription)
            putExtra(AlarmClock.EXTRA_DAYS, lesson.course.dayOfWeek)
            putExtra(AlarmClock.EXTRA_HOUR, alarmTime.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.minute)
        }
        // 根据闹钟设置方式选择设置了直接返回课程表还是停留在闹钟界面
        when (SharedPreferenceConfig.getInt(requireContext(), SharedPreferenceConfig.KEY_ALARM_WAY)) {
            SharedPreferenceConfig.AlarmWay.WAY_DIRECTLY -> {
                intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            }

            SharedPreferenceConfig.AlarmWay.WAY_NEED_CONFIRM -> {
                intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "设置闹钟失败，系统内可能没有闹钟app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setCalendarSchedule() {
        try {
            val context = requireContext()
            val reminderMinutes = SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_REMINDER_TIME)
            val endMillis = lesson.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, lesson.name)
                put(CalendarContract.Events.DESCRIPTION, reminderDescription)
                put(CalendarContract.Events.DTSTART, reminderTime)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            if (uri != null) {
                Toast.makeText(context, "已成功加入日历，将在上课前${reminderMinutes} 分钟提醒你", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "加入日历失败", Toast.LENGTH_SHORT).show()
        }
    }
}