package com.example.sleepwell.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.sleepwell.data.models.AlarmSettings
import com.example.sleepwell.data.models.AppLockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sleep_well_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        private val ALARM_HOUR = intPreferencesKey("alarm_hour")
        private val ALARM_MINUTE = intPreferencesKey("alarm_minute")
        private val LOCKOUT_DURATION = intPreferencesKey("lockout_duration")
        private val SELECTED_APPS = stringSetPreferencesKey("selected_apps")

        private val LOCK_ACTIVE = booleanPreferencesKey("lock_active")
        private val LOCK_START_TIME = longPreferencesKey("lock_start_time")
        private val LOCK_END_TIME = longPreferencesKey("lock_end_time")
        private val LOCKED_APPS = stringSetPreferencesKey("locked_apps")
    }

    val alarmSettings: Flow<AlarmSettings> = context.dataStore.data.map { preferences ->
        AlarmSettings(
            isEnabled = preferences[ALARM_ENABLED] ?: false,
            alarmHour = preferences[ALARM_HOUR] ?: 7,
            alarmMinute = preferences[ALARM_MINUTE] ?: 0,
            lockoutDurationMinutes = preferences[LOCKOUT_DURATION] ?: 30,
            selectedApps = preferences[SELECTED_APPS] ?: emptySet()
        )
    }

    val appLockState: Flow<AppLockState> = context.dataStore.data.map { preferences ->
        AppLockState(
            isActive = preferences[LOCK_ACTIVE] ?: false,
            startTime = preferences[LOCK_START_TIME] ?: 0L,
            endTime = preferences[LOCK_END_TIME] ?: 0L,
            lockedApps = preferences[LOCKED_APPS] ?: emptySet()
        )
    }

    suspend fun updateAlarmSettings(settings: AlarmSettings) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_ENABLED] = settings.isEnabled
            preferences[ALARM_HOUR] = settings.alarmHour
            preferences[ALARM_MINUTE] = settings.alarmMinute
            preferences[LOCKOUT_DURATION] = settings.lockoutDurationMinutes
            preferences[SELECTED_APPS] = settings.selectedApps
        }
    }

    suspend fun updateSelectedApps(apps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_APPS] = apps
        }
    }

    suspend fun updateAppLockState(lockState: AppLockState) {
        context.dataStore.edit { preferences ->
            preferences[LOCK_ACTIVE] = lockState.isActive
            preferences[LOCK_START_TIME] = lockState.startTime
            preferences[LOCK_END_TIME] = lockState.endTime
            preferences[LOCKED_APPS] = lockState.lockedApps
        }
    }

    suspend fun setAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_ENABLED] = enabled
        }
    }
}