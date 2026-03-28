package com.lff.classschedule

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {

    companion object{
        val TAG = "HomeActivity"
    }

    private lateinit var btnMore: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnMore = findViewById(R.id.btn_more)
        setupPopupMenu(btnMore)

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