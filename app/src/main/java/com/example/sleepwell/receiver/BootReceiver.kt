package com.example.sleepwell.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sleepwell.data.repository.SleepWellRepository
import com.example.sleepwell.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            val repository = SleepWellRepository(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarmSettings = repository.alarmSettings.first()
                    if (alarmSettings.isEnabled) {
                        // Reschedule alarm
                        AlarmService.scheduleAlarm(context, alarmSettings)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}