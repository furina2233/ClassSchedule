package com.lff.classschedule

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.util.PermissionUtil

class AppSettingsActivity : AppCompatActivity() {

    companion object{
        const val TAG = "AppSettingsActivity"
    }

        private lateinit var etTermCommencementTimeMonth: EditText
        private lateinit var etTermCommencementTimeDay: EditText
        private lateinit var etSetMaxWeeks: EditText
        private lateinit var etSetDurationPerLesson: EditText
        private lateinit var etSetReminderTime: EditText
        private lateinit var spnSetRemindWay: Spinner
        private lateinit var tvCurrentLessons: TextView
        private lateinit var etStartTimes: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)

        etTermCommencementTimeMonth = findViewById(R.id.et_term_commencement_time_month)
        etTermCommencementTimeDay = findViewById(R.id.et_term_commencement_time_day)
        etSetMaxWeeks = findViewById(R.id.et_set_max_weeks)
        etSetDurationPerLesson = findViewById(R.id.et_set_duration_per_lesson)
        etSetReminderTime = findViewById(R.id.et_set_reminder_time)
        spnSetRemindWay = findViewById(R.id.spn_set_remind_way)
        tvCurrentLessons = findViewById(R.id.tv_current_lessons)
        etStartTimes = findViewById(R.id.et_start_times)

        setupEtTermCommencementTimeMonth()
        setupEtTermCommencementTimeDay()
        setupEtSetMaxWeeks()
        setupEtSetDurationPerLesson()
        setupEtSetReminderTime()
        setupSpnSetRemindWay()
        setupTvCurrentLessons()
        setupEtStartTimes()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (isShouldHideInput(v, ev)) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v?.windowToken, 0)  // 收起输入法软键盘
                v?.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isShouldHideInput(v: View?, event: MotionEvent): Boolean {
        if (v != null && (v is EditText || v is TextInputEditText)) {
            val leftTop = intArrayOf(0, 0)
            // 获取输入框在屏幕上的位置
            v.getLocationInWindow(leftTop)
            val left = leftTop[0]
            val top = leftTop[1]
            val bottom = top + v.height
            val right = left + v.width
            // 判断点击坐标是否在输入框区域内
            return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
        }
        return false
    }

    private fun setupEtTermCommencementTimeMonth() {
        etTermCommencementTimeMonth.setText(SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_MONTH).toString())
        etTermCommencementTimeMonth.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus){
                val month = etTermCommencementTimeMonth.text.toString().toInt()
                if (month !in 1..12){
                    Toast.makeText(this,"请输入正确的月份", Toast.LENGTH_SHORT).show()
                }else{
                    SharedPreferenceConfig.setInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_MONTH, month)
                }
            }
        }
    }

    private fun setupEtTermCommencementTimeDay() {
        etTermCommencementTimeDay.setText(SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_DAY).toString())
        etTermCommencementTimeDay.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus){
                val day = etTermCommencementTimeDay.text.toString().toInt()
                if (day !in 1..31){
                    Toast.makeText(this,"请输入正确的天数", Toast.LENGTH_SHORT).show()
                }else{
                    SharedPreferenceConfig.setInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_DAY, day)
                }
            }
        }
    }

    private fun setupEtSetMaxWeeks() {
        etSetMaxWeeks.setText(SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_MAX_WEEKS).toString())
        etSetMaxWeeks.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus){
                val maxWeeks = etSetMaxWeeks.text.toString().toInt()
                if (maxWeeks !in 1..53){
                    Toast.makeText(this,"请输入正确的最大周数", Toast.LENGTH_SHORT).show()
                }else{
                    SharedPreferenceConfig.setInt(this, SharedPreferenceConfig.KEY_MAX_WEEKS, maxWeeks)
                }
            }
        }
    }

    private fun setupEtSetDurationPerLesson() {
        etSetDurationPerLesson.setText(SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_DURATION_PER_LESSON).toString())
        etSetDurationPerLesson.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus){
                val durationPerLesson = etSetDurationPerLesson.text.toString().toInt()
                if (durationPerLesson !in 1..1440){
                    Toast.makeText(this,"请输入正确的课程时长", Toast.LENGTH_SHORT).show()
                }else{
                    SharedPreferenceConfig.setInt(this, SharedPreferenceConfig.KEY_DURATION_PER_LESSON, durationPerLesson)
                }
            }
        }
    }

    private fun setupEtSetReminderTime() {
        etSetReminderTime.setText(SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_REMINDER_TIME).toString())
        etSetReminderTime.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus){
                val reminderTime = etSetReminderTime.text.toString().toInt()
                if (reminderTime !in 0..1440){
                    Toast.makeText(this,"请输入正确的提醒时间", Toast.LENGTH_SHORT).show()
                }else{
                    SharedPreferenceConfig.setInt(this, SharedPreferenceConfig.KEY_REMINDER_TIME, reminderTime)
                }
            }
        }
    }

    private fun setupSpnSetRemindWay() {
        val remindWay = arrayOf("日历日程", "闹钟", "通知")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, remindWay)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnSetRemindWay.adapter = adapter

        spnSetRemindWay.setSelection(SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_REMIND_WAY))
        spnSetRemindWay.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, selectedView: View?, position: Int, id: Long) {
                when(position){
                    SharedPreferenceConfig.RemindWay.WAY_CALENDAR_SCHEDULE -> {
                        if (!PermissionUtil.hasCalendarPermission(this@AppSettingsActivity)){
                            val message = "需要读写日历权限，否则无法设置日程。"
                            PermissionUtil.goToCalendarSettings(this@AppSettingsActivity, message)
                        }
                    }
                    SharedPreferenceConfig.RemindWay.WAY_ALARM -> {}  // 通过系统闹钟app设置闹钟权限不需要用户手动授予
                    // TODO:需要进入设置页面时就触发且如果两个权限都缺失，设置了其中一个之后还要再触发
                    SharedPreferenceConfig.RemindWay.WAY_NOTIFICATION -> {
                        if (!PermissionUtil.hasNotificationPermission(this@AppSettingsActivity)){
                            val message = "需要通知权限和精确闹钟权限，否则无法提醒你。\n注意：请将app的省电策略调整为无限制。否则也无法提醒你。"
                            PermissionUtil.goToNotificationSettings(this@AppSettingsActivity, message)
                        }
                    }
                }
                SharedPreferenceConfig.setInt(this@AppSettingsActivity, SharedPreferenceConfig.KEY_REMIND_WAY, position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTvCurrentLessons() {
        tvCurrentLessons.text = SharedPreferenceConfig.getString(this, SharedPreferenceConfig.KEY_START_TIMES).split(",").size.toString()
    }

    private fun setupEtStartTimes() {
        etStartTimes.setText(SharedPreferenceConfig.getString(this, SharedPreferenceConfig.KEY_START_TIMES).split(",").joinToString("\n"))
        etStartTimes.onFocusChangeListener = View.OnFocusChangeListener{v, hasFocus ->
            if (!hasFocus){
                val startTimes = etStartTimes.text.toString().split("\n").toTypedArray().map {
                    it.trim().replace("：", ":")
                }.filter { it.isNotEmpty()
                }.toTypedArray()
                try {
                    for (time in startTimes){
                        val timeArray = time.split(":")
                        val h = timeArray[0].toInt()
                        val m = timeArray[1].toInt()
                        if (h !in 0..23 || m !in 0..59) {
                            throw Exception()
                        }
                    }
                }catch (e: Exception){
                    Toast.makeText(this, "请输入正确的开始时间", Toast.LENGTH_SHORT).show()
                    return@OnFocusChangeListener
                }
                SharedPreferenceConfig.setString(this, SharedPreferenceConfig.KEY_START_TIMES, startTimes.joinToString(","))
            }
        }
    }
}