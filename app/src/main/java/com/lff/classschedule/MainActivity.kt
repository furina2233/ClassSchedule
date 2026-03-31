package com.lff.classschedule

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.lff.classschedule.config.SharedPreferenceConfig

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkFirstLaunch()
        finish()
    }

    fun checkFirstLaunch() {
        val isFirstLaunch = SharedPreferenceConfig.getBoolean(this, SharedPreferenceConfig.KEY_IS_FIRST_LAUNCH)
        if (isFirstLaunch) {
            Log.d(TAG, "第一次启动，转到欢迎页")
            startActivity(Intent(this, WelcomeActivity::class.java))  // 转到欢迎页
            SharedPreferenceConfig.setBoolean(this, SharedPreferenceConfig.KEY_IS_FIRST_LAUNCH, false)
        } else {
            Log.d(TAG, "非第一次启动，转到主页")
            startActivity(Intent(this, HomeActivity::class.java))  // 转到主页
        }
    }
}