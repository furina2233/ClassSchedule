package com.lff.classschedule

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkFirstLaunch()
        finish()
    }

    fun checkFirstLaunch() {
        val sharedPreferences = getSharedPreferences("class_schedule_config", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
        if (true) {  // 暂时设置为一定是第一次启动
            Log.d(TAG,"第一次启动，转到欢迎页")
            startActivity(Intent(this, WelcomeActivity::class.java))  // 转到欢迎页
            sharedPreferences.edit { putBoolean("is_first_launch", false) }
        }else{
            Log.d(TAG,"非第一次启动，转到主页")
            startActivity(Intent(this, HomeActivity::class.java))  // 转到主页
        }
    }
}