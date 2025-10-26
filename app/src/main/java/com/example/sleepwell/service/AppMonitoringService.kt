package com.example.sleepwell.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.sleepwell.MainActivity
import com.example.sleepwell.R
import com.example.sleepwell.data.repository.SleepWellRepository
import com.example.sleepwell.overlay.AppBlockOverlayActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitoringService : Service() {

    companion object {
        private const val CHANNEL_ID = "SLEEPWELL_MONITORING_CHANNEL"
        private const val NOTIFICATION_ID = 1002
        private const val CHECK_INTERVAL = 500L // Check every 500ms for responsiveness
        private const val OVERLAY_COOLDOWN = 1000L // 1 second cooldown to prevent loops
    }

    private lateinit var repository: SleepWellRepository
    private var serviceJob: Job? = null
    private var isMonitoring = false
    private var lastCheckedApp: String = ""
    private var currentlyBlockedApp: String = ""
    private var overlayShownTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        repository = SleepWellRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring) {
                try {
                    val lockState = repository.appLockState.first()

                    if (!lockState.isActive || System.currentTimeMillis() >= lockState.endTime) {
                        // Lock period has ended, stop monitoring
                        stopSelf()
                        break
                    }

                    val currentApp = getCurrentForegroundApp()
                    android.util.Log.d("SleepWell", "Current foreground app: $currentApp")

                    if (currentApp != null &&
                        lockState.lockedApps.contains(currentApp) &&
                        currentApp != packageName &&  // Don't block our own app
                        !currentApp.startsWith("com.example.sleepwell")) { // Don't block our overlay

                        // Only show overlay if we haven't shown it recently for this app
                        val currentTime = System.currentTimeMillis()
                        if (currentlyBlockedApp != currentApp ||
                            currentTime - overlayShownTime > OVERLAY_COOLDOWN) {

                            android.util.Log.d("SleepWell", "Blocking app: $currentApp")
                            showBlockOverlay(currentApp)
                            currentlyBlockedApp = currentApp
                            overlayShownTime = currentTime
                        }
                    } else if (currentApp != null) {
                        // Reset tracking when user switches to non-locked app
                        if (!lockState.lockedApps.contains(currentApp)) {
                            currentlyBlockedApp = ""
                            overlayShownTime = 0
                        }
                    }

                    // Use minimal delay for maximum responsiveness
                    delay(CHECK_INTERVAL)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(CHECK_INTERVAL)
                }
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        serviceJob?.cancel()
        serviceJob = null
    }

    private fun getCurrentForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 2 // Last 2 seconds for immediate detection

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            var lastEvent: UsageEvents.Event? = null

            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)

                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastEvent = event
                }
            }

            return lastEvent?.packageName
        } catch (e: Exception) {
            android.util.Log.e("SleepWell", "Failed to get current foreground app", e)
            return null
        }
    }

    private fun showBlockOverlay(packageName: String) {
        try {
            val intent = Intent(this, AppBlockOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("blocked_package", packageName)
            }
            startActivity(intent)
            android.util.Log.d("SleepWell", "Overlay launched for: $packageName")
        } catch (e: Exception) {
            android.util.Log.e("SleepWell", "Failed to show overlay", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Monitoring"
            val descriptionText = "Monitoring for locked apps"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SleepWell")
            .setContentText("App monitoring active")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}