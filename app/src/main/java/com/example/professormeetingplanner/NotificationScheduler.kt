package com.example.professormeetingplanner

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

//object NotificationScheduler {
//
//    fun scheduleNotification(context: Context, notificationTime: Calendar, title: String, message: String) {
//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val intent = Intent(context, NotificationReceiver::class.java).apply {
//            putExtra("title", title)
//            putExtra("message", message)
//        }
//        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime.timeInMillis, pendingIntent)
//    }
//}

object NotificationScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(context: Context, notificationTime: Calendar, title: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }
        val requestCode = notificationTime.timeInMillis.toInt() // Unique request code
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime.timeInMillis, pendingIntent)
    }
}
