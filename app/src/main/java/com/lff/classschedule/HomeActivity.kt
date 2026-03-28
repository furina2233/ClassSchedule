package com.lff.classschedule

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.lff.classschedule.config.SharedPreferenceConfig
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HomeActivity : AppCompatActivity() {

    companion object{
        val TAG = "HomeActivity"
    }

    private lateinit var btnMore: ImageButton

    private lateinit var spinnerSelectWeek: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnMore = findViewById(R.id.btn_more)
        setupPopupMenu(btnMore)
    }

    override fun onResume() {
        super.onResume()

        // 当前周选择，放在onResume()中才能使修改开学日期并返回后立刻刷新当前周数
        spinnerSelectWeek = findViewById(R.id.spinner_select_week)
        setupSpinnerSelectWeek()
    }

    private fun setupSpinnerSelectWeek() {
        val maxWeeks = SharedPreferenceConfig.getMaxWeeksPerSemester(this)
        val weeksArray = Array(maxWeeks) { "第${it + 1}周" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weeksArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSelectWeek.adapter = adapter

        val termCommencementTimeMonth = SharedPreferenceConfig.getTermCommencementTimeMonth(this)
        val termCommencementTimeDay = SharedPreferenceConfig.getTermCommencementTimeDay(this)

        val currentWeek = calculateCurrentWeek(termCommencementTimeMonth, termCommencementTimeDay)

        val selectionIndex = (currentWeek - 1).coerceIn(0, maxWeeks - 1)
        spinnerSelectWeek.setSelection(selectionIndex)
    }

    private fun calculateCurrentWeek(startMonth: Int, startDay: Int): Int {
        val today = LocalDate.now()
        var startDoc = LocalDate.of(today.year, startMonth, startDay)

        // 如果开学日期（如9月）比今天（如1月）晚，说明跨年了
        if (startDoc.isAfter(today.plusMonths(1))) {
            startDoc = startDoc.minusYears(1)
        }

        // 对齐周一，当学期开始日期不是周一时，使下一个周一为第二周的开始
        val firstMonday = startDoc.with(DayOfWeek.MONDAY)

        // 计算天数差
        val daysBetween = ChronoUnit.DAYS.between(firstMonday, today)

        // 计算周数
        val currentWeek = (daysBetween / 7).toInt() + 1

        return currentWeek
    }

    fun setupPopupMenu(btnMore: ImageButton) {
        btnMore.setOnClickListener { v ->
            val popupMenu = PopupMenu(this, v)
            popupMenu.menuInflater.inflate(R.menu.home_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_course_setting -> {
                        startActivity(Intent(this, CoursesSettingActivity::class.java))
                        true
                    }
                    R.id.menu_about -> {
                        TODO("关于页面")
                        true
                    }else -> false
                }
            }
            popupMenu.show()
        }
    }

}