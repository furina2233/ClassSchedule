package com.lff.classschedule

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import java.text.SimpleDateFormat
import java.util.*


class AboutActivity : AppCompatActivity() {

    companion object {
        const val TAG = "AboutActivity"
    }

    private lateinit var tvVersionName: TextView
    private lateinit var tvWebsite: TextView
    private lateinit var tvLastBuildTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        tvVersionName = findViewById(R.id.tv_version_name)
        tvVersionName.text = BuildConfig.VERSION_NAME

        tvLastBuildTime = findViewById(R.id.tv_last_build_time)
        val lastBuildTimeMillis = BuildConfig.BUILD_TIME
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
        tvLastBuildTime.text = format.format(lastBuildTimeMillis)

        tvWebsite = findViewById(R.id.tv_website)
        val linkText = "<a href='https://github.com/furina2233/ClassSchedule'>点击前往</a>"
        tvWebsite.text = HtmlCompat.fromHtml(linkText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        tvWebsite.movementMethod = LinkMovementMethod.getInstance()
    }
}