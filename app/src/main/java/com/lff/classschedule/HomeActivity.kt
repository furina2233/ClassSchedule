package com.lff.classschedule

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.database.CourseDbHelper
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.pojo.Lesson
import com.lff.classschedule.util.ColorUtil
import com.lff.classschedule.util.ViewUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HomeActivity : AppCompatActivity() {

    companion object{
        const val TAG = "HomeActivity"
    }

    private lateinit var btnMore: ImageButton

    private lateinit var spinnerSelectWeek: Spinner

    private lateinit var dbHelper: CourseDbHelper

    private lateinit var courseSet: MutableSet<Course>
    private val lessonMap: MutableMap<DayOfWeek,List<Lesson>> = mutableMapOf()
    private var currentWeek: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dbHelper = CourseDbHelper(this)

        btnMore = findViewById(R.id.btn_more)
        setupPopupMenu(btnMore)
    }

    override fun onResume() {
        super.onResume()

        ColorUtil.resetColorPool()

        // 当前周选择
        spinnerSelectWeek = findViewById(R.id.spinner_select_week)
        setupSpinnerSelectWeek()

        courseSet = dbHelper.queryAllCourses().values.toMutableSet()
        Log.d(TAG,"查询到的课程为：$courseSet")

        loadClassScheduleTable()

        refreshSchedule()
    }

    private fun fillClassSchedule() {
        val columnLayouts = listOf<LinearLayout>(
            findViewById(R.id.ll_monday_container),
            findViewById(R.id.ll_tuesday_container),
            findViewById(R.id.ll_wednesday_container),
            findViewById(R.id.ll_thursday_container),
            findViewById(R.id.ll_friday_container),
            findViewById(R.id.ll_saturday_container),
            findViewById(R.id.ll_sunday_container)
        )

        val maxLessons = SharedPreferenceConfig.getMaxLessonsPerDay(this)

        for (i in 1..7) {
            val dayOfWeek = DayOfWeek.of(i)
            val dayLayout = columnLayouts[i - 1]
            dayLayout.removeAllViews() // 清空旧视图

            // 获取这一天的课程并按开始节数排序
            val dayLessons = lessonMap[dayOfWeek]?.sortedBy { it.startLesson } ?: emptyList()

            var currentLesson = 1

            // 3. 填充这一天的每一节课位
            for (lesson in dayLessons) {
                // 无课判断
                if (lesson.startLesson > currentLesson) {
                    val gapSize = lesson.startLesson - currentLesson
                    addEmptyView(dayLayout, gapSize)
                }

                addCourseCard(dayLayout, lesson)
                currentLesson = lesson.endLesson + 1
            }
            // 无课判断
            if (currentLesson <= maxLessons) {
                addEmptyView(dayLayout, maxLessons - currentLesson + 1)
            }
        }
    }

    private fun addCourseCard(container: LinearLayout, lesson: Lesson) {
        val card = layoutInflater.inflate(R.layout.item_class_schedule_lesson_card, container, false)
        val tvLessonInfo = card.findViewById<TextView>(R.id.tv_lesson_info)

        tvLessonInfo.text = lesson.description

        // 设置卡片颜色
        val courseColor = ColorUtil.getColor(lesson.name)
        val flCard = card.findViewById<View>(R.id.fl_lesson_card)
        flCard.background.setTint(courseColor)

        // 设置卡片高度
        val params = card.layoutParams as LinearLayout.LayoutParams
        val duration = lesson.endLesson - lesson.startLesson + 1
        params.height = ViewUtil.getLessonHeightPx(this, duration)
        params.setMargins(2, 2, 2, 2)  // 理论上应该跟空白view一样，但是实际上会对不齐
        card.layoutParams = params

        card.setOnClickListener {
            Toast.makeText(this, "查看课程: ${lesson.name}", Toast.LENGTH_SHORT).show()
        }

        container.addView(card)
    }

    private fun addEmptyView(container: LinearLayout, duration: Int) {
        val emptyView = View(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            ViewUtil.getLessonHeightPx(this, duration)
        )
        params.setMargins(1, 1, 1, 1)
        emptyView.layoutParams = params

        container.addView(emptyView)
    }

    private fun loadClassScheduleTable() {
        val llColumnHeader = findViewById<LinearLayout>(R.id.ll_column_header)
        llColumnHeader.removeAllViews()
        for (i in 1..SharedPreferenceConfig.getMaxLessonsPerDay(this)) {
            val headerItem = layoutInflater.inflate(R.layout.item_class_schedule_column_header,llColumnHeader,false)
            headerItem.findViewById<TextView>(R.id.tv_column_header).text = "$i"
            llColumnHeader.addView(headerItem)
        }

    }

    private fun buildLessonMap() {
        for (dayOfWeek in 1..7){
            val lessons = mutableListOf<Lesson>()
            val iterator = courseSet.iterator()
            while (iterator.hasNext()) {
                val course = iterator.next()
                if (course.dayOfWeek == dayOfWeek&& currentWeek >= course.startWeek && currentWeek <= course.endWeek) {
                    lessons.add(
                        Lesson(
                            course.name,
                            course.startLesson,
                            course.endLesson,
                            DayOfWeek.of(course.dayOfWeek),
                            "${course.name}@${course.location}"
                        )
                    )
                }
            }
            lessons.sortBy { it.startLesson }
            lessonMap[DayOfWeek.of(dayOfWeek)] = lessons
        }
        Log.d(TAG,"构建的课表为：$lessonMap")
    }

    private fun setupSpinnerSelectWeek() {
        val maxWeeks = SharedPreferenceConfig.getMaxWeeksPerSemester(this)
        val weeksArray = Array(maxWeeks) { "第${it + 1}周" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weeksArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSelectWeek.adapter = adapter

        val termCommencementTimeMonth = SharedPreferenceConfig.getTermCommencementTimeMonth(this)
        val termCommencementTimeDay = SharedPreferenceConfig.getTermCommencementTimeDay(this)

        currentWeek = calculateCurrentWeek(termCommencementTimeMonth, termCommencementTimeDay)

        spinnerSelectWeek.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedWeek = position + 1
                if (currentWeek != selectedWeek) {
                    currentWeek = selectedWeek

                    // 重新刷新 UI
                    refreshSchedule()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        val selectionIndex = (currentWeek - 1).coerceIn(0, maxWeeks - 1)
        spinnerSelectWeek.setSelection(selectionIndex)
    }

    private fun refreshSchedule() {
        ColorUtil.resetColorPool()
        buildLessonMap()
        fillClassSchedule()
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

    private fun setupPopupMenu(btnMore: ImageButton) {
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