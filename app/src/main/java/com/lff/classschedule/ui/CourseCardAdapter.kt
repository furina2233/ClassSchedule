package com.lff.classschedule.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lff.classschedule.R
import com.lff.classschedule.pojo.Course
import com.lff.classschedule.util.CourseTimeUtil

class CourseCardAdapter(
    private val onEdit: (courseId: Int, course: Course) -> Unit,
    private val onDelete: (courseId: Int, courseName: String) -> Unit,
    private val onBatchDeleteSelected: (courseId: Int) -> Unit,
    private val isBatchDeleteMode: () -> Boolean
) : ListAdapter<Pair<Int, Course>, CourseCardAdapter.ViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<Pair<Int, Course>>() {
        override fun areItemsTheSame(oldItem: Pair<Int, Course>, newItem: Pair<Int, Course>): Boolean =
            oldItem.first == newItem.first

        override fun areContentsTheSame(oldItem: Pair<Int, Course>, newItem: Pair<Int, Course>): Boolean =
            oldItem.second == newItem.second
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val itemView: android.view.View
    ) : RecyclerView.ViewHolder(itemView) {

        private val clCard: ConstraintLayout = itemView.findViewById(R.id.cl_card)
        private val tvCourseName: TextView = itemView.findViewById(R.id.tv_course_name)
        private val tvCourseWeeks: TextView = itemView.findViewById(R.id.tv_course_weeks)
        private val tvCourseTime: TextView = itemView.findViewById(R.id.tv_course_time)
        private val tvCourseLocation: TextView = itemView.findViewById(R.id.tv_course_location)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btn_more)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(entry: Pair<Int, Course>) {
            val (courseId, course) = entry
            val context = itemView.context

            clCard.tag = courseId.toString()

            tvCourseName.text = course.name

            val weekText = "${course.startWeek}-${course.endWeek} 周"
            tvCourseWeeks.text = weekText

            val timeText = CourseTimeUtil.getDayOfWeekText(course.dayOfWeek) + "  " +
                    CourseTimeUtil.getTimeStringByStartAndEndClassIndex(
                        context, course.startLesson, course.endLesson
                    )
            tvCourseTime.text = timeText

            tvCourseLocation.text = course.location

            if (isBatchDeleteMode()) {
                btnDelete.visibility = android.view.View.VISIBLE
                btnMore.visibility = android.view.View.INVISIBLE
                clCard.setBackgroundResource(R.drawable.bg_course_card)

                btnDelete.setOnClickListener {
                    val isSelected = it.tag as? Boolean ?: false
                    if (!isSelected) {
                        clCard.setBackgroundResource(R.drawable.bg_to_delete_course_card)
                        it.tag = true
                    } else {
                        clCard.setBackgroundResource(R.drawable.bg_course_card)
                        it.tag = false
                    }
                    onBatchDeleteSelected(courseId)
                }
            } else {
                btnDelete.visibility = android.view.View.GONE
                btnMore.visibility = android.view.View.VISIBLE
                clCard.setBackgroundResource(R.drawable.bg_course_card)
                btnDelete.tag = false

                btnMore.setOnClickListener { v ->
                    val popup = PopupMenu(context, v)
                    popup.menuInflater.inflate(R.menu.course_item_menu, popup.menu)
                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_edit -> {
                                onEdit(courseId, course)
                                true
                            }
                            R.id.menu_delete -> {
                                onDelete(courseId, course.name)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }
            }
        }
    }
}
