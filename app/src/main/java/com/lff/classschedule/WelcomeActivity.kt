package com.lff.classschedule

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import androidx.core.content.edit

class WelcomeActivity : AppCompatActivity() {

    var TAG = "WelcomeActivity"

    lateinit var btnSkip : MaterialButton  // 跳过键
    lateinit var btnGo : MaterialButton  // 开始使用键

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btnSkip = findViewById(R.id.btn_skip)
        btnGo = findViewById(R.id.btn_go)

        btnSkip.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            val sharedPreferences = getSharedPreferences("class_schedule_config", MODE_PRIVATE)
            sharedPreferences.edit { putBoolean("is_first_launch", false) }
            finish()
        }
        btnGo.setOnClickListener {
            startActivity(Intent(this, CoursesSettingActivity::class.java))
            val sharedPreferences = getSharedPreferences("class_schedule_config", MODE_PRIVATE)
            sharedPreferences.edit { putBoolean("is_first_launch", false) }
            finish()
        }
        Log.d(TAG, "onCreate: 跳过键和开始使用键已初始化")
    }
}