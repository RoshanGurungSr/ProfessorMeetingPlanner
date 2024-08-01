package com.example.professormeetingplanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Appointment Reminder"
        val message = intent.getStringExtra("message") ?: "You have an upcoming appointment."

        NotificationHelper.createNotification(context, title, message)
    }
}
