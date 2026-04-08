package com.lff.classschedule

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.lff.classschedule.config.SharedPreferenceConfig

class WelcomeActivity : AppCompatActivity() {


    companion object {
        const val TAG = "WelcomeActivity"
    }

    lateinit var btnSkip: MaterialButton  // 跳过键
    lateinit var btnGo: MaterialButton  // 开始使用键

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btnSkip = findViewById(R.id.btn_skip)
        btnGo = findViewById(R.id.btn_go)

        btnSkip.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            SharedPreferenceConfig.setBoolean(this, SharedPreferenceConfig.KEY_IS_FIRST_LAUNCH, false)
            finish()
        }
        btnGo.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            startActivity(Intent(this, CoursesSettingActivity::class.java))
            startActivity(Intent(this, AppSettingsActivity::class.java))
            SharedPreferenceConfig.setBoolean(this, SharedPreferenceConfig.KEY_IS_FIRST_LAUNCH, false)
            finish()
        }
        Log.d(TAG, "onCreate: 跳过键和开始使用键已初始化")
    }
}