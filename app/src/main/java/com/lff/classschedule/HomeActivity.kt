package com.lff.classschedule

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.database.CourseDbHelper
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.pojo.Lesson
import com.lff.classschedule.receiver.DesktopWidgetProvider
import com.lff.classschedule.ui.LessonInfoDialog
import com.lff.classschedule.util.ColorUtil
import com.lff.classschedule.util.CourseTimeUtil
import com.lff.classschedule.util.ViewUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HomeActivity : AppCompatActivity() {

    companion object {
        const val TAG = "HomeActivity"
    }

    private lateinit var firstMonday: LocalDate  // 开学后的第一个周一的日期
    private lateinit var btnMore: ImageButton

    private lateinit var spinnerSelectWeek: Spinner

    private lateinit var dbHelper: CourseDbHelper

    private lateinit var courseSet: MutableSet<Course>
    private val lessonMap: MutableMap<DayOfWeek, List<Lesson>> = mutableMapOf()
    private var currentWeek: Int = 0
    private lateinit var tvMonth: TextView
    private lateinit var dateTextViews: List<TextView>
    private val courseColorMap = mutableMapOf<Course, Int>() // 更新取色方式，使得课程在整个学期的颜色一致

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dbHelper = CourseDbHelper(this)

        btnMore = findViewById(R.id.btn_more)
        setupPopupMenu(btnMore)

        tvMonth = findViewById(R.id.tv_month)
        dateTextViews = listOf(
            findViewById(R.id.tv_monday_day),
            findViewById(R.id.tv_tuesday_day),
            findViewById(R.id.tv_wednesday_day),
            findViewById(R.id.tv_thursday_day),
            findViewById(R.id.tv_friday_day),
            findViewById(R.id.tv_saturday_day),
            findViewById(R.id.tv_sunday_day)
        )
    }

    override fun onResume() {
        super.onResume()

        ColorUtil.resetColorPool()
        getFirstMonday()

        // 当前周选择
        spinnerSelectWeek = findViewById(R.id.spinner_select_week)
        setupSpinnerSelectWeek()

        courseSet = dbHelper.queryAllCourses().values.toMutableSet()
        for (course in courseSet){
            courseColorMap[course] = ColorUtil.getColor(course.name)
        }

        Log.d(TAG, "查询到的课程为：$courseSet")

        loadClassScheduleTable()

        refreshSchedule()

        refreshDesktopWidget()
    }

    private fun refreshDesktopWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetName = ComponentName(this, DesktopWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetName)

        val intent = Intent(this, DesktopWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        sendBroadcast(intent)
    }

    fun getFirstMonday() {
        val startMonth = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_MONTH)
        val startDay = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_DAY)

        val today = LocalDate.now()
        var startDoc = LocalDate.of(today.year, startMonth, startDay)
        if (startDoc.isAfter(today.plusMonths(1))) {
            startDoc = startDoc.minusYears(1)
        }
        firstMonday = startDoc.with(DayOfWeek.MONDAY)
    }

    private fun updateDateHeader() {
        // 计算当前选中周的周一日期
        val currentMonday = firstMonday.plusWeeks((currentWeek - 1).toLong())
        // 设置月份，如果跨月，以周一所属月份为准
        tvMonth.text = "${currentMonday.monthValue}月"

        for (i in 0..6) {
            val dateOfRow = currentMonday.plusDays(i.toLong())
            dateTextViews[i].text = dateOfRow.dayOfMonth.toString()

            val parentLayout = dateTextViews[i].parent as LinearLayout  // 放置周几和该日日期的容器
            val dayOfWeekTextView = parentLayout.getChildAt(0) as TextView
            val column = findViewById<LinearLayout>(R.id.ll_column_container).getChildAt(i)  // 放置当日课程的容器

            // 如果是今天，显示高亮
            if (dateOfRow == LocalDate.now()) {
                dateTextViews[i].setTextColor(getColor(R.color.high_light_foreground))
                parentLayout.setBackgroundColor(getColor(R.color.high_light_background))
                dayOfWeekTextView.setTextColor(getColor(R.color.high_light_foreground))
                column.setBackgroundColor(getColor(R.color.high_light_background))
            } else {
                dateTextViews[i].setTextColor(getColor(R.color.gray))
                dayOfWeekTextView.setTextColor(getColor(R.color.title_text))
                parentLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                column.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }
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

        val maxLessons = SharedPreferenceConfig.getString(this, SharedPreferenceConfig.KEY_START_TIMES).split(",").size

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
                    addEmptyCard(dayLayout, gapSize)
                }

                addCourseCard(dayLayout, lesson)
                currentLesson = lesson.endLesson + 1
            }
            // 无课判断
            if (currentLesson <= maxLessons) {
                addEmptyCard(dayLayout, maxLessons - currentLesson + 1)
            }
        }
    }

    private fun addCourseCard(container: LinearLayout, lesson: Lesson) {
        val card = layoutInflater.inflate(R.layout.item_class_schedule_lesson_card, container, false)
        val tvLessonInfo = card.findViewById<TextView>(R.id.tv_lesson_info)

        tvLessonInfo.text = lesson.description

        // 设置卡片颜色
        val courseColor = courseColorMap[lesson.course]!!
        val flCard = card.findViewById<View>(R.id.fl_lesson_card)
        flCard.background.setTint(courseColor)

        // 设置卡片高度
        val params = card.layoutParams as LinearLayout.LayoutParams
        val duration = lesson.endLesson - lesson.startLesson + 1
        params.height = ViewUtil.getLessonHeightPx(this, duration)
        card.layoutParams = params

        card.setOnClickListener {
            val dialog = LessonInfoDialog.newInstance(lesson)
            dialog.show(supportFragmentManager, LessonInfoDialog.TAG)
        }

        container.addView(card)
    }

    private fun addEmptyCard(container: LinearLayout, duration: Int) {
        val card = layoutInflater.inflate(R.layout.item_class_schedule_lesson_empty_card, container, false)

        val params = card.layoutParams as LinearLayout.LayoutParams
        params.height = ViewUtil.getLessonHeightPx(this, duration)
        card.layoutParams = params

        container.addView(card)
    }

    private fun loadClassScheduleTable() {
        val llColumnHeader = findViewById<LinearLayout>(R.id.ll_column_header)
        llColumnHeader.removeAllViews()
        for (i in 1..SharedPreferenceConfig.getString(this, SharedPreferenceConfig.KEY_START_TIMES).split(",").size) {
            val headerItem = layoutInflater.inflate(R.layout.item_class_schedule_column_header, llColumnHeader, false)
            headerItem.findViewById<TextView>(R.id.tv_column_header).text = "$i"
            llColumnHeader.addView(headerItem)
        }

    }

    private fun buildLessonMap() {
        for (dayOfWeek in 1..7) {
            val lessons = mutableListOf<Lesson>()
            val iterator = courseSet.iterator()

            // 公式：第一周周一 + (选中周 - 1) * 7 + (星期几 - 1)
            val targetDate = firstMonday
                .plusWeeks((currentWeek - 1).toLong())
                .plusDays((dayOfWeek - 1).toLong())


            while (iterator.hasNext()) {
                val course = iterator.next()
                if (course.dayOfWeek == dayOfWeek && currentWeek >= course.startWeek && currentWeek <= course.endWeek) {
                    val timeRange = CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                        this, course.startLesson, course.endLesson
                    )
                    val startTimeStr = timeRange.split("-")[0]
                    val (hour, minute) = startTimeStr.split(":").map { it.toInt() }
                    val lessonDateTime = targetDate.atTime(hour, minute)

                    lessons.add(
                        Lesson(
                            course.name,
                            course.startLesson,
                            course.endLesson,
                            DayOfWeek.of(course.dayOfWeek),
                            "${course.name}@${course.location}",
                            lessonDateTime,
                            course
                        )
                    )
                }
            }
            lessons.sortBy { it.startLesson }
            lessonMap[DayOfWeek.of(dayOfWeek)] = lessons
        }
        Log.d(TAG, "构建的课表为：$lessonMap")
    }

    private fun setupSpinnerSelectWeek() {
        val maxWeeks = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_MAX_WEEKS)
        val weeksArray = Array(maxWeeks) { "第${it + 1}周" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weeksArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSelectWeek.adapter = adapter

        currentWeek = calculateCurrentWeek()

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
        updateDateHeader()
        buildLessonMap()
        fillClassSchedule()
    }

    private fun calculateCurrentWeek(): Int {
        val today = LocalDate.now()

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
                        val intent = Intent(this, CoursesSettingActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        true
                    }

                    R.id.menu_app_settings -> {
                        val intent = Intent(this, AppSettingsActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        true
                    }

                    R.id.menu_about -> {
                        val intent = Intent(this, AboutActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        true
                    }

                    else -> false
                }
            }
            popupMenu.show()
        }
    }

}