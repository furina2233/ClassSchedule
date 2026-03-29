package com.lff.classschedule.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.util.ScreenUtil

class SetStartTimesDialog : DialogFragment() {
    companion object {
        const val TAG = "SetStartTimesDialog"
    }

    private lateinit var llContainer: LinearLayout
    private val hourList = (0..23).map { it.toString().padStart(2, '0') }
    private val minuteList = (0..59).map { it.toString().padStart(2, '0') }

    private lateinit var startTimes: MutableList<String>
    private lateinit var btnSave: MaterialButton
    private lateinit var btnAdd: MaterialButton
    private lateinit var btnSort: MaterialButton
    private lateinit var scrollView: NestedScrollView

    private var cardOnSelected: View? = null


    override fun onStart() {
        super.onStart()
        ScreenUtil.setDialogWidth(this, requireContext(), 0.9f)
        ScreenUtil.setDialogMaxHeight(this, requireContext(), 0.7f)
        ScreenUtil.setDialogMinHeight(this, requireContext(), 0.5f)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_set_start_times, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        startTimes = SharedPreferenceConfig.getStartTimes(context).toMutableList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llContainer = view.findViewById(R.id.ll_set_start_times)
        scrollView = view.findViewById(R.id.scroll_view)

        val currentMaxLessons = SharedPreferenceConfig.getMaxLessonsPerDay(requireContext())
        Log.d(TAG, "当前的课程开始时间是：${startTimes.joinToString(",")}")
        for (i in 1..currentMaxLessons) {
            addLessonTimeCard(i)
        }

        btnAdd = view.findViewById(R.id.btn_add)
        btnAdd.setOnClickListener {
            onAddButtonClick()
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

        btnSave = view.findViewById(R.id.btn_save)
        btnSave.setOnClickListener {
            performSave()
        }

        btnSort = view.findViewById(R.id.btn_sort)
        btnSort.setOnClickListener {
            onSortButtonClick()
        }
    }

    private fun onSortButtonClick() {
        // 表示需要恢复选中状态的时间
        val time = cardOnSelected?.let {
            val h = it.findViewById<Spinner>(R.id.spinner_start_time_hour).selectedItem.toString()
            val m = it.findViewById<Spinner>(R.id.spinner_start_time_minute).selectedItem.toString()
            "$h:$m"
        }

        llContainer.removeAllViews()
        sortStartTimes()
        for (i in 1..startTimes.size) {
            addLessonTimeCard(i)
            if (time == startTimes[i - 1]) {
                Log.d(TAG, "需要恢复选中状态的卡片的时间为：$time")
                setCardOnSelected(llContainer.getChildAt(i - 1))
            }
        }

        scrollView.post {
            val targetY = cardOnSelected?.let { it.top - scrollView.height / 2 + it.height / 2 }
            val maxScrollY = (scrollView.getChildAt(0).height - scrollView.height).coerceAtLeast(0)
            Log.d(TAG, "maxScrollY = $maxScrollY")
            if (targetY != null && maxScrollY >= targetY) {
                scrollView.smoothScrollTo(0, targetY)
            } else {
                scrollView.smoothScrollTo(0, ScrollView.FOCUS_UP)
            }
        }

    }

    private fun onAddButtonClick() {
        addLessonTimeCard(llContainer.childCount + 1)
    }

    private fun addLessonTimeCard(lessonIndex: Int) {
        val cardView = layoutInflater.inflate(R.layout.item_start_time_card, llContainer, false)

        val isNewLesson = lessonIndex > startTimes.size
        if (isNewLesson) {
            startTimes.add("08:00")
            setCardOnSelected(cardView)
        }

        // 设置标题
        cardView.findViewById<TextView>(R.id.tv_start_time_card_title).text = "第 $lessonIndex 节"

        // 获取上课时间
        val startTime = (if (isNewLesson) "08:00" else startTimes[lessonIndex - 1]).split(":")

        // 配置小时 Spinner
        val spHour = cardView.findViewById<Spinner>(R.id.spinner_start_time_hour)
        spHour.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, hourList)
        spHour.setSelection(startTime[0].toInt())
        spHour.post {
            spHour.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    setCardOnSelected(cardView)

                    val hour = hourList[position]
                    val newStartTime = "$hour:${startTimes[lessonIndex - 1].split(":")[1]}"
                    startTimes[lessonIndex - 1] = newStartTime
                    Log.d(TAG, "第 $lessonIndex 节开始时间已更新为：$newStartTime")
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }


        // 配置分钟 Spinner
        val spMinute = cardView.findViewById<Spinner>(R.id.spinner_start_time_minute)
        spMinute.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, minuteList)
        spMinute.setSelection(startTime[1].toInt())
        spMinute.post {
            spMinute.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    setCardOnSelected(cardView)

                    val newStartTime = "${startTimes[lessonIndex - 1].split(":")[0]}:${minuteList[position]}"
                    startTimes[lessonIndex - 1] = newStartTime
                    Log.d(TAG, "第 $lessonIndex 节开始时间已更新为：$newStartTime")
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }

        // 配置删除按钮
        cardView.findViewById<View>(R.id.btn_delete).setOnClickListener {
            llContainer.removeView(cardView)
            startTimes.removeAt(lessonIndex - 1)
            reorderTitles() // 删除后重新排序标题
        }

        llContainer.addView(cardView)
    }


    private fun setCardOnSelected(newCardOnSelected: View) {
        Log.d(TAG, "新的选中的卡片为：${newCardOnSelected.findViewById<TextView>(R.id.tv_start_time_card_title).text}")
        cardOnSelected?.setBackgroundResource(R.drawable.bg_course_card)
        cardOnSelected = newCardOnSelected
        cardOnSelected?.setBackgroundResource(R.drawable.bg_selected_course_card)
    }

    private fun reorderTitles() {
        for (i in 0 until llContainer.childCount) {
            val child = llContainer.getChildAt(i)
            val tvTitle = child.findViewById<TextView>(R.id.tv_start_time_card_title)
            tvTitle.text = "第 ${i + 1} 节"
        }
    }

    private fun performSave() {
        sortStartTimes()
        parentFragmentManager.setFragmentResult(TAG, Bundle().apply {
            putStringArray("new_start_times", startTimes.toTypedArray())
        })
        dismiss()
    }

    private fun sortStartTimes() {
        startTimes.sortBy {
            it.split(":")[0].toInt() * 60 + it.split(":")[1].toInt()
        }
    }
}
