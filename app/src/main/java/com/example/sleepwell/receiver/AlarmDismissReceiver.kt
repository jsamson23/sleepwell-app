package com.example.sleepwell.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Cancel the alarm notification
        with(NotificationManagerCompat.from(context)) {
            cancel(1001) // Same ID as in AlarmReceiver
        }
    }
}