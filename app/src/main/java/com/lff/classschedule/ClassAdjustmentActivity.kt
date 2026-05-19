package com.lff.classschedule

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lff.classschedule.database.Adjustment
import com.lff.classschedule.database.AppDatabase
import com.lff.classschedule.ui.AddAdjustmentDialogFragment
import com.lff.classschedule.ui.AdjustmentAdapter
import com.lff.classschedule.util.HolidayScheduleFetcher
import com.lff.classschedule.viewmodel.AdjustmentsViewModel

class ClassAdjustmentActivity : AppCompatActivity() {

    private lateinit var viewModel: AdjustmentsViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdjustmentAdapter
    private lateinit var tvNoAdjustments: TextView
    private lateinit var btnAddAdjustment: FloatingActionButton
    private lateinit var btnAdjustmentMenu: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_adjustment)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AdjustmentsViewModel(AppDatabase.getInstance(this@ClassAdjustmentActivity).adjustmentDao()) as T
            }
        })[AdjustmentsViewModel::class.java]

        tvNoAdjustments = findViewById(R.id.tv_no_adjustments)
        recyclerView = findViewById(R.id.rv_adjustments)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdjustmentAdapter(
            onDelete = { adjustment ->
                showDeleteConfirmDialog(adjustment)
            },
            onActiveChanged = { adjustment, isActive ->
                viewModel.setAdjustmentActive(adjustment.id, isActive)
            }
        )
        recyclerView.adapter = adapter

        btnAddAdjustment = findViewById(R.id.btn_add_adjustment)
        btnAddAdjustment.setOnClickListener {
            supportFragmentManager.setFragmentResultListener(
                AddAdjustmentDialogFragment.TAG, this
            ) { _, _ ->
                viewModel.refreshAdjustments()
            }
            val dialog = AddAdjustmentDialogFragment()
            dialog.show(supportFragmentManager, AddAdjustmentDialogFragment.TAG)
        }

        btnAdjustmentMenu = findViewById(R.id.btn_adjustment_menu)
        btnAdjustmentMenu.setOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menuInflater.inflate(R.menu.class_adjustment_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_auto_adjust -> {
                        showAutoAdjustDialog()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        viewModel.adjustments.observe(this) { adjustments ->
            adapter.submitList(adjustments)
            if (adjustments.isEmpty()) {
                tvNoAdjustments.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvNoAdjustments.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.refreshAdjustments()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAdjustments()
    }

    private fun showDeleteConfirmDialog(adjustment: Adjustment) {
        AlertDialog.Builder(this)
            .setTitle("删除调课")
            .setMessage("确定要删除「${adjustment.description}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteAdjustment(adjustment)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAutoAdjustDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_auto_adjust, null)
        val switchAuto = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_auto_adjust)
        val btnRunNow = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_run_auto_adjust)

        val dialog = AlertDialog.Builder(this)
            .setTitle("自动调课")
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .create()

        btnRunNow.setOnClickListener {
            dialog.dismiss()
            performAutoAdjust()
        }

        dialog.show()
    }

    private fun performAutoAdjust() {
        val adjustments = HolidayScheduleFetcher.fetchAdjustments(this)
        if (adjustments.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("自动调课")
                .setMessage("未获取到需要调课的信息")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        for (adj in adjustments) {
            viewModel.addAdjustment(adj)
        }

        AlertDialog.Builder(this)
            .setTitle("自动调课")
            .setMessage("已成功添加 ${adjustments.size} 条调课记录")
            .setPositiveButton("确定") { _, _ -> viewModel.refreshAdjustments() }
            .show()
    }
}
