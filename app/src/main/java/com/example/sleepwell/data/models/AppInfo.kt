package com.example.sleepwell.data.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val isSelected: Boolean = false
)

data class AlarmSettings(
    val isEnabled: Boolean = false,
    val alarmHour: Int = 7,
    val alarmMinute: Int = 0,
    val lockoutDurationMinutes: Int = 30,
    val selectedApps: Set<String> = emptySet()
)

data class AppLockState(
    val isActive: Boolean = false,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val lockedApps: Set<String> = emptySet()
)