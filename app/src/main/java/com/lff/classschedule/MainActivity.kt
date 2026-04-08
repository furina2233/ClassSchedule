package com.lff.classschedule

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lff.classschedule.config.SharedPreferenceConfig

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkFontScale()

    }

    private fun checkFontScale() {
        val fontScale = resources.configuration.fontScale
        if (fontScale >= 1.2f) {
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("当前系统字体缩放比例过大，可能会导致部分文字显示不全，请将系统字体缩放比例调小。")
                .setPositiveButton("去调整") { _, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("取消", { _, _ ->
                    checkFirstLaunch()
                })
                .show()
        } else {
            checkFirstLaunch()
        }
    }

    private fun checkFirstLaunch() {
        val isFirstLaunch = SharedPreferenceConfig.getBoolean(this, SharedPreferenceConfig.KEY_IS_FIRST_LAUNCH)
        if (isFirstLaunch) {
            Log.d(TAG, "第一次启动，转到欢迎页")
            startActivity(Intent(this, WelcomeActivity::class.java))  // 转到欢迎页
            SharedPreferenceConfig.setBoolean(this, SharedPreferenceConfig.KEY_IS_FIRST_LAUNCH, false)
        } else {
            Log.d(TAG, "非第一次启动，转到主页")
            startActivity(Intent(this, HomeActivity::class.java))  // 转到主页
        }
        finish()
    }
}