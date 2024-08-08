package com.example.professormeetingplanner

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object NotificationScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(context: Context, notificationTime: Calendar, title: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        // Schedule the notification 10 minutes before the actual appointment time
        val notificationTimeInMillis = notificationTime.timeInMillis - 10 * 60 * 1000
        println("We have " + notificationTime.time)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTimeInMillis, pendingIntent)
        //NotificationHelper.createNotification(context, title, message)

    }

    fun scheduleNotificationForBoth(context: Context, notificationTime: Calendar, appointment: Appointment) {
        val title = "Upcoming Appointment"
        val messageForStudent = "You have an appointment with ${appointment.studentName} for ${appointment.courseName}."
        val messageForProfessor = "You have an appointment with ${appointment.email} for ${appointment.courseName}."

        scheduleNotification(context, notificationTime, title, messageForStudent)
        scheduleNotification(context, notificationTime, title, messageForProfessor)
    }
}
