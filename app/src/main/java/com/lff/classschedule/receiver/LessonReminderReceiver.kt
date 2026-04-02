package com.lff.classschedule.receiver

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.os.bundleOf
import com.lff.classschedule.R
import com.lff.classschedule.config.SharedPreferenceConfig
import com.lff.classschedule.pojo.Lesson
import com.lff.classschedule.util.CourseTimeUtil
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.core.net.toUri
import com.lff.classschedule.util.PermissionUtil

class LessonReminderReceiver: BroadcastReceiver() {

    companion object{
        const val TAG = "LessonReminderReceiver"
        const val CHANNEL_ID = "lesson_reminder_channel"

        fun setLessonReminder(context: Context, lesson: Lesson): String {
            if (!PermissionUtil.hasNotificationPermission(context)){
                val message = "需要通知权限和精确闹钟权限，否则无法提醒你。\n注意：请将app的省电策略调整为无限制。否则也无法提醒你。"
                PermissionUtil.goToNotificationSettings(context as Activity,message)
                return "请先开启通知权限"
            }
            return performSetAlarm(context, lesson)
        }

        @SuppressLint("ScheduleExactAlarm")  // 在PermissionUtil中进行检查，不在这里检查
        private fun performSetAlarm(context: Context, lesson: Lesson): String {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val description = CourseTimeUtil.getTimeStringByStartAndEndClassIndex(context, lesson.startLesson, lesson.endLesson) +
                    " 在 " + lesson.course.location + " 上 " + lesson.name + " 课。"

            val intent = Intent(context, LessonReminderReceiver::class.java).apply {
                putExtra("notificationId", lesson.hashCode())
                putExtra("title", lesson.name)
                putExtra("content", description)
            }

            // 用课程名称加当前年份的哈希作为requestCode，防止给同个课程的多次课设置提醒时被覆盖
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (lesson.name + LocalDateTime.now().dayOfYear.toString()).hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val reminderTime = lesson.startTime.minusMinutes(SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_REMINDER_TIME).toLong())
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (reminderTime < System.currentTimeMillis()){
                return "这节课已经上过啦！"
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
            return "将在上课前${SharedPreferenceConfig.getInt(context, SharedPreferenceConfig.KEY_REMINDER_TIME)}分钟提醒你"
        }

        private fun showNotificationPermissionDialog(context: Context) {
            AlertDialog.Builder(context)
                .setTitle("需要权限")
                .setMessage("请在设置中开启通知权限，否则无法提醒你。")
                .setPositiveButton("去开启") { _, _ ->
                    val intent = Intent().apply {
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            else -> {
                                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                putExtra("app_package", context.packageName)
                                putExtra("app_uid", context.applicationInfo.uid)
                            }
                        }
                    }
                    context.startActivity(intent)
                }
                .setNegativeButton("取消", null)
                .show()
                .apply {
                    getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.resources.getColor(R.color.main_theme, null))
                }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val channelId = CHANNEL_ID
        val notificationId = intent.getIntExtra("notificationId", 0)
        val title = intent.getStringExtra("title")
        val content = intent.getStringExtra("content")
        Log.d(TAG, "收到提醒：$notificationId，$title，$content")

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, "上课提醒", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }
}