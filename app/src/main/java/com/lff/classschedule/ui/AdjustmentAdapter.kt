package com.lff.classschedule.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lff.classschedule.R
import com.lff.classschedule.database.Adjustment

class AdjustmentAdapter(
    private val onDelete: (Adjustment) -> Unit,
    private val onActiveChanged: (Adjustment, Boolean) -> Unit
) : ListAdapter<Adjustment, AdjustmentAdapter.ViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<Adjustment>() {
        override fun areItemsTheSame(oldItem: Adjustment, newItem: Adjustment): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Adjustment, newItem: Adjustment): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_adjustment_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val container: android.view.View
    ) : RecyclerView.ViewHolder(container) {

        private val tvDesc = container.findViewById<android.widget.TextView>(R.id.tv_adjustment_desc)
        private val tvOrigTime = container.findViewById<android.widget.TextView>(R.id.tv_original_time)
        private val tvNewTime = container.findViewById<android.widget.TextView>(R.id.tv_new_time)
        private val switchActive = container.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_active)
        private val btnDelete = container.findViewById<android.widget.ImageButton>(R.id.btn_delete_adjustment)

        private val dayNames = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")

        fun bind(adjustment: Adjustment) {
            tvDesc.text = adjustment.description

            tvOrigTime.text = "原时间：${dayNames[adjustment.originalDayOfWeek]} 第${adjustment.originalStartLesson}-${adjustment.originalEndLesson}节"

            tvNewTime.text = "调整后：${dayNames[adjustment.newDayOfWeek]} 第${adjustment.newStartLesson}-${adjustment.newEndLesson}节"

            switchActive.setOnCheckedChangeListener(null)
            switchActive.isChecked = adjustment.isActive
            switchActive.setOnCheckedChangeListener { _, isChecked ->
                onActiveChanged(adjustment, isChecked)
            }

            btnDelete.setOnClickListener {
                onDelete(adjustment)
            }
        }
    }
}
