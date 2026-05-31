package com.lff.classschedule

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.database.AppDatabase
import com.lff.classschedule.viewmodel.ScheduleViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lff.classschedule.receiver.DesktopWidgetProvider
import com.lff.classschedule.ui.WeekScheduleFragment
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HomeActivity : AppCompatActivity() {

    companion object {
        const val TAG = "HomeActivity"
    }

    private lateinit var btnMore: ImageButton
    private lateinit var spinnerSelectWeek: Spinner
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: WeekPagerAdapter

    val viewModel: ScheduleViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(this@HomeActivity)
                @Suppress("UNCHECKED_CAST")
                return ScheduleViewModel(db.courseDao(), db.adjustmentDao()) as T
            }
        }
    }

    private lateinit var fabGoHome: FloatingActionButton
    private var currentWeek: Int = 0
    private var actualCurrentWeek: Int = 1
    private var spinnerListenerAttached = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnMore = findViewById(R.id.btn_more)
        setupPopupMenu(btnMore)

        spinnerSelectWeek = findViewById(R.id.spinner_select_week)
        viewPager = findViewById(R.id.vp_week_schedule)

        viewPagerAdapter = WeekPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        fabGoHome = findViewById(R.id.fab_go_home)
        fabGoHome.setOnClickListener {
            currentWeek = actualCurrentWeek
            viewPager.setCurrentItem(actualCurrentWeek - 1, false)
            syncSpinnerToViewPager(actualCurrentWeek - 1)
            fabGoHome.visibility = View.GONE
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val selectedWeek = position + 1
                if (currentWeek != selectedWeek) {
                    currentWeek = selectedWeek
                    syncSpinnerToViewPager(position)
                }
                fabGoHome.visibility = if (selectedWeek == actualCurrentWeek) View.GONE else View.VISIBLE
            }
        })
    }

    override fun onResume() {
        super.onResume()

        viewModel.setConfig(
            maxWeeks = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_MAX_WEEKS),
            month = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_MONTH),
            day = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_DAY),
            startTimes = SharedPreferenceConfig.getString(this, SharedPreferenceConfig.KEY_START_TIMES)
        )
        viewModel.refreshData()

        val maxWeeks = viewModel.maxWeeks.value
            ?: SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_MAX_WEEKS)

        setupSpinnerSelectWeek(maxWeeks)

        actualCurrentWeek = calculateCurrentWeek()
        currentWeek = actualCurrentWeek

        val pageIndex = (currentWeek - 1).coerceIn(0, maxWeeks - 1)
        viewPagerAdapter.setItemCount(maxWeeks)
        viewPagerAdapter.refresh()
        viewPager.setCurrentItem(pageIndex, false)

        syncSpinnerToViewPager(pageIndex)

        refreshDesktopWidget()
    }

    private fun syncSpinnerToViewPager(position: Int) {
        spinnerListenerAttached = false
        if (spinnerSelectWeek.selectedItemPosition != position) {
            spinnerSelectWeek.setSelection(position)
        }
        spinnerListenerAttached = true
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

    private fun calculateCurrentWeek(): Int {
        val firstMonday = viewModel.getFirstMonday() ?: run {
            val startMonth = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_MONTH)
            val startDay = SharedPreferenceConfig.getInt(this, SharedPreferenceConfig.KEY_TERM_COMMENCEMENT_TIME_DAY)
            val today = LocalDate.now()
            var startDoc = LocalDate.of(today.year, startMonth, startDay)
            if (startDoc.isAfter(today.plusMonths(1))) startDoc = startDoc.minusYears(1)
            startDoc.with(java.time.DayOfWeek.MONDAY)
        }
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(firstMonday, today)
        return (daysBetween / 7).toInt() + 1
    }

    private fun setupSpinnerSelectWeek(maxWeeks: Int) {
        val weeksArray = Array(maxWeeks) { "第${it + 1}周" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weeksArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSelectWeek.adapter = adapter

        spinnerSelectWeek.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!spinnerListenerAttached) return
                val selectedWeek = position + 1
                if (currentWeek != selectedWeek) {
                    currentWeek = selectedWeek
                    viewPager.setCurrentItem(position, false)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupPopupMenu(btnMore: ImageButton) {
        btnMore.setOnClickListener { v ->
            val popupMenu = android.widget.PopupMenu(this, v)
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

                    R.id.menu_class_adjustment -> {
                        val intent = Intent(this, ClassAdjustmentActivity::class.java)
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

    private class WeekPagerAdapter(activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

        private var itemCount: Int = 18
        private var refreshVersion: Long = 0

        fun setItemCount(count: Int) {
            if (itemCount != count) {
                itemCount = count
                notifyDataSetChanged()
            }
        }

        fun refresh() {
            refreshVersion++
            notifyDataSetChanged()
        }

        override fun getItemId(position: Int): Long =
            position + refreshVersion * 1000

        override fun containsItem(itemId: Long): Boolean {
            val position = (itemId % 1000).toInt()
            return position in 0 until itemCount
        }

        override fun getItemCount(): Int = itemCount

        override fun createFragment(position: Int): WeekScheduleFragment {
            return WeekScheduleFragment.newInstance(position + 1)
        }
    }
}
