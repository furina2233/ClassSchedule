package com.lff.classschedule.util

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lff.classschedule.R

object PermissionUtil {

    fun hasCalendarPermission(context: Context): Boolean {
        val permissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms())
    }

    fun goToCalendarSettings(activity: Activity, message: String) {
        showDialog(activity, message) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        }
    }

    fun goToNotificationSettings(activity: Activity, message: String) {
        val isNotifyEnabled = NotificationManagerCompat.from(activity).areNotificationsEnabled()

        if (!isNotifyEnabled) {
            showDialog(activity, message) {
                val intent = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                        }
                    }
                    else -> {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", activity.packageName, null)
                        }
                    }
                }
                activity.startActivity(intent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showDialog(activity, message) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
                }
            }
        }
    }

    private fun showDialog(context: Context, message: String, onConfirm: () -> Unit) {
        val activity = context as? Activity ?: return

        AlertDialog.Builder(activity)
            .setTitle("需要权限")
            .setMessage(message)
            .setNegativeButton("取消", null)
            .setPositiveButton("去开启") { _, _ -> onConfirm() }
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(activity.getColor(R.color.main_theme))
            }
    }
}