package com.example.sleepwell.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepwell.data.models.AlarmSettings
import com.example.sleepwell.data.repository.SleepWellRepository
import com.example.sleepwell.service.AlarmService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val alarmSettings: AlarmSettings = AlarmSettings(),
    val isLoading: Boolean = true,
    val showTimePicker: Boolean = false,
    val showDurationPicker: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SleepWellRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.alarmSettings.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    alarmSettings = settings,
                    isLoading = false
                )
            }
        }
    }

    fun showTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = true)
    }

    fun hideTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = false)
    }

    fun showDurationPicker() {
        _uiState.value = _uiState.value.copy(showDurationPicker = true)
    }

    fun hideDurationPicker() {
        _uiState.value = _uiState.value.copy(showDurationPicker = false)
    }

    fun updateAlarmTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.alarmSettings
            val updatedSettings = currentSettings.copy(
                alarmHour = hour,
                alarmMinute = minute
            )
            repository.updateAlarmSettings(updatedSettings)

            // Reschedule alarm if enabled
            if (updatedSettings.isEnabled) {
                AlarmService.scheduleAlarm(getApplication(), updatedSettings)
            }
        }
    }

    fun updateLockoutDuration(minutes: Int) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.alarmSettings
            val updatedSettings = currentSettings.copy(
                lockoutDurationMinutes = minutes
            )
            repository.updateAlarmSettings(updatedSettings)

            // Reschedule alarm if enabled to update the duration
            if (updatedSettings.isEnabled) {
                AlarmService.scheduleAlarm(getApplication(), updatedSettings)
            }
        }
    }
}