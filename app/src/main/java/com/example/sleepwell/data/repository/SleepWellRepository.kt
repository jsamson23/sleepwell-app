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

        // Get all installed packages with activities
        val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)

        return installedPackages
            .filter { packageInfo ->
                val packageName = packageInfo.packageName

                // Skip our own app and excluded packages
                if (packageName == context.packageName || excludedPackages.contains(packageName)) {
                    return@filter false
                }

                // Check if app has launcher activity (can be opened by user)
                val hasLauncherActivity = hasLauncherActivity(packageManager, packageName)

                // Include if it has a launcher activity (this covers all user-launchable apps)
                hasLauncherActivity
            }
            .mapNotNull { packageInfo ->
                try {
                    val packageName = packageInfo.packageName
                    val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                    val icon = packageInfo.applicationInfo.loadIcon(packageManager)

                    // Skip apps with empty or system-like names
                    if (appName.isBlank() || appName.startsWith("com.")) {
                        return@mapNotNull null
                    }

                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                        isSelected = selectedApps.contains(packageName)
                    )
                } catch (e: Exception) {
                    android.util.Log.w("SleepWell", "Failed to load app info for package", e)
                    null // Skip apps that can't be loaded
                }
            }
            .sortedBy { it.appName }
    }

    private fun hasLauncherActivity(packageManager: PackageManager, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        return packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }

    private fun getExcludedSystemPackages(): Set<String> {
        // Only exclude critical system packages that shouldn't be blocked
        return setOf(
            "com.android.settings",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.android.phone",
            "com.android.contacts",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.sec.android.app.launcher", // Samsung launcher
            "com.samsung.android.incallui", // Samsung phone
            "com.android.emergency", // Emergency dialer
            "android", // Core Android system
            context.packageName // Our own app
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