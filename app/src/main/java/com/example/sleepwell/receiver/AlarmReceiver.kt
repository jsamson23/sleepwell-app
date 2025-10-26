package com.example.sleepwell.receiver

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sleepwell.MainActivity
import com.example.sleepwell.R
import com.example.sleepwell.data.models.AppLockState
import com.example.sleepwell.data.repository.SleepWellRepository
import com.example.sleepwell.service.AppMonitoringService
import com.example.sleepwell.ui.alarm.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "SLEEPWELL_ALARM_CHANNEL"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("SleepWell", "AlarmReceiver triggered!")

        val repository = SleepWellRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmSettings = repository.alarmSettings.first()
                android.util.Log.d("SleepWell", "Alarm settings: enabled=${alarmSettings.isEnabled}")

                if (alarmSettings.isEnabled) {
                    // Create notification channel
                    createNotificationChannel(context)

                    // Launch alarm activity with sound
                    launchAlarmActivity(context)
                    android.util.Log.d("SleepWell", "Alarm activity launched")

                    // Start app lockout
                    startAppLockout(context, repository, alarmSettings)
                    android.util.Log.d("SleepWell", "App lockout started")
                }
            } catch (e: Exception) {
                android.util.Log.e("SleepWell", "Error in AlarmReceiver", e)
                e.printStackTrace()
            }
        }
    }

    private fun launchAlarmActivity(context: Context) {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                   Intent.FLAG_ACTIVITY_CLEAR_TOP or
                   Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }

    private suspend fun startAppLockout(
        context: Context,
        repository: SleepWellRepository,
        alarmSettings: com.example.sleepwell.data.models.AlarmSettings
    ) {
        if (alarmSettings.selectedApps.isNotEmpty()) {
            val currentTime = System.currentTimeMillis()
            val endTime = currentTime + (alarmSettings.lockoutDurationMinutes * 60 * 1000)

            val lockState = AppLockState(
                isActive = true,
                startTime = currentTime,
                endTime = endTime,
                lockedApps = alarmSettings.selectedApps
            )

            repository.updateAppLockState(lockState)

            // Start monitoring service
            val serviceIntent = Intent(context, AppMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SleepWell Alarms"
            val descriptionText = "Alarm notifications from SleepWell"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showAlarmNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SleepWell Alarm")
            .setContentText("Good morning! Your apps will be locked for the set duration.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                "Dismiss",
                dismissPendingIntent
            )
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 300, 300, 300))

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}