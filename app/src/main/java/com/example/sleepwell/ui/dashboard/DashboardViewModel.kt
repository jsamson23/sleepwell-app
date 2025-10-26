package com.example.sleepwell.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepwell.data.models.AlarmSettings
import com.example.sleepwell.data.models.AppLockState
import com.example.sleepwell.data.repository.SleepWellRepository
import com.example.sleepwell.service.AlarmService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DashboardUiState(
    val alarmSettings: AlarmSettings = AlarmSettings(),
    val appLockState: AppLockState = AppLockState(),
    val formattedAlarmTime: String = "",
    val formattedUnlockTime: String = "",
    val lockTimeRemaining: String = "",
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SleepWellRepository(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.alarmSettings,
                repository.appLockState
            ) { alarmSettings, appLockState ->
                _uiState.value = _uiState.value.copy(
                    alarmSettings = alarmSettings,
                    appLockState = appLockState,
                    formattedAlarmTime = formatTime(alarmSettings.alarmHour, alarmSettings.alarmMinute),
                    formattedUnlockTime = calculateUnlockTime(alarmSettings),
                    lockTimeRemaining = calculateLockTimeRemaining(appLockState),
                    isLoading = false
                )
            }.collect { }
        }
    }

    fun toggleAlarmEnabled() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.alarmSettings
            val newEnabled = !currentSettings.isEnabled

            repository.setAlarmEnabled(newEnabled)

            if (newEnabled) {
                // Schedule alarm
                AlarmService.scheduleAlarm(getApplication(), currentSettings.copy(isEnabled = true))
            } else {
                // Cancel alarm
                AlarmService.cancelAlarm(getApplication())
            }
        }
    }

    fun testAlarm() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.alarmSettings
            // Schedule a test alarm for 10 seconds from now
            val testSettings = currentSettings.copy(
                isEnabled = true,
                alarmHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                alarmMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
            )

            // Add 1 minute for testing
            val testCalendar = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.MINUTE, 1)
            }
            val testSettingsWithTime = testSettings.copy(
                alarmHour = testCalendar.get(java.util.Calendar.HOUR_OF_DAY),
                alarmMinute = testCalendar.get(java.util.Calendar.MINUTE)
            )

            AlarmService.scheduleAlarm(getApplication(), testSettingsWithTime)
            android.util.Log.d("SleepWell", "Test alarm scheduled for 1 minute from now")
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(calendar.time)
    }

    private fun calculateUnlockTime(alarmSettings: AlarmSettings): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarmSettings.alarmHour)
            set(Calendar.MINUTE, alarmSettings.alarmMinute)
            add(Calendar.MINUTE, alarmSettings.lockoutDurationMinutes)
        }
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(calendar.time)
    }

    private fun calculateLockTimeRemaining(appLockState: AppLockState): String {
        if (!appLockState.isActive || System.currentTimeMillis() >= appLockState.endTime) {
            return ""
        }

        val remainingMs = appLockState.endTime - System.currentTimeMillis()
        val remainingMinutes = (remainingMs / (1000 * 60)).toInt()
        val hours = remainingMinutes / 60
        val minutes = remainingMinutes % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m remaining"
        } else {
            "${minutes}m remaining"
        }
    }
}