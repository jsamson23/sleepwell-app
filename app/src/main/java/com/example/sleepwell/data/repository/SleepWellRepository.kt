package com.example.sleepwell.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.example.sleepwell.data.datastore.PreferencesManager
import com.example.sleepwell.data.models.AlarmSettings
import com.example.sleepwell.data.models.AppInfo
import com.example.sleepwell.data.models.AppLockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SleepWellRepository(private val context: Context) {
    private val preferencesManager = PreferencesManager(context)

    val alarmSettings: Flow<AlarmSettings> = preferencesManager.alarmSettings
    val appLockState: Flow<AppLockState> = preferencesManager.appLockState

    suspend fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val selectedApps = alarmSettings.first().selectedApps
        val excludedPackages = getExcludedSystemPackages()

        // Get ALL apps that have launcher activities (this is the most reliable way)
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val launcherApps = packageManager.queryIntentActivities(launcherIntent, 0)

        return launcherApps
            .mapNotNull { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName

                    // Skip our own app and critical system packages
                    if (packageName == context.packageName || excludedPackages.contains(packageName)) {
                        return@mapNotNull null
                    }

                    val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(applicationInfo).toString()
                    val icon = applicationInfo.loadIcon(packageManager)

                    // Skip apps with empty or generic names
                    if (appName.isBlank() || appName == packageName) {
                        return@mapNotNull null
                    }

                    android.util.Log.d("SleepWell", "Found app: $appName ($packageName)")

                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                        isSelected = selectedApps.contains(packageName)
                    )
                } catch (e: Exception) {
                    android.util.Log.w("SleepWell", "Failed to load app info", e)
                    null
                }
            }
            .distinctBy { it.packageName } // Remove duplicates
            .sortedBy { it.appName }
    }

    private fun getExcludedSystemPackages(): Set<String> {
        // Only exclude the most critical system components that should NEVER be blocked
        return setOf(
            "com.android.settings", // System Settings
            "com.android.systemui", // System UI
            "com.android.launcher", // Default launcher
            "com.android.launcher3", // Launcher3
            "com.google.android.apps.nexuslauncher", // Pixel launcher
            "com.sec.android.app.launcher", // Samsung launcher
            "com.miui.home", // MIUI launcher
            "com.huawei.android.launcher", // Huawei launcher
            "com.android.phone", // Phone app
            "com.android.dialer", // Dialer
            "com.google.android.dialer", // Google Dialer
            "com.samsung.android.incallui", // Samsung phone
            "com.android.emergency", // Emergency dialer
            "android" // Core Android system
        )
    }

    suspend fun updateAlarmSettings(settings: AlarmSettings) {
        preferencesManager.updateAlarmSettings(settings)
    }

    suspend fun updateSelectedApps(apps: Set<String>) {
        preferencesManager.updateSelectedApps(apps)
    }

    suspend fun updateAppLockState(lockState: AppLockState) {
        preferencesManager.updateAppLockState(lockState)
    }

    suspend fun setAlarmEnabled(enabled: Boolean) {
        preferencesManager.setAlarmEnabled(enabled)
    }

    suspend fun isAppLocked(packageName: String): Boolean {
        val lockState = appLockState.first()
        return lockState.isActive &&
               lockState.lockedApps.contains(packageName) &&
               System.currentTimeMillis() < lockState.endTime
    }

    suspend fun getCurrentLockTimeRemaining(): Long {
        val lockState = appLockState.first()
        return if (lockState.isActive && System.currentTimeMillis() < lockState.endTime) {
            lockState.endTime - System.currentTimeMillis()
        } else {
            0L
        }
    }
}