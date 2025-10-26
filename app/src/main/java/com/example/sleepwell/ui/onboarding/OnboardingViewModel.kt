package com.example.sleepwell.ui.onboarding

import android.app.AlarmManager
import android.app.Application
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionState(
    val isGranted: Boolean,
    val isRequired: Boolean,
    val title: String,
    val description: String,
    val actionText: String,
    val settingsAction: String? = null
)

data class OnboardingUiState(
    val currentStep: Int = 0,
    val permissions: List<PermissionState> = emptyList(),
    val allPermissionsGranted: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        updatePermissionStates()
    }

    fun updatePermissionStates() {
        val context = getApplication<Application>()
        val permissions = listOf(
            PermissionState(
                isGranted = hasUsageStatsPermission(context),
                isRequired = true,
                title = "Usage Access Permission",
                description = "SleepWell needs to monitor which apps you're using to block them during lockout periods.",
                actionText = "Grant Usage Access",
                settingsAction = Settings.ACTION_USAGE_ACCESS_SETTINGS
            ),
            PermissionState(
                isGranted = hasOverlayPermission(context),
                isRequired = true,
                title = "Display Over Other Apps",
                description = "This allows SleepWell to show a blocking screen when you try to open locked apps.",
                actionText = "Allow Display Over Apps",
                settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            ),
            PermissionState(
                isGranted = hasExactAlarmPermission(context),
                isRequired = true,
                title = "Schedule Exact Alarms",
                description = "This ensures your alarm rings at the exact time you set, even in battery optimization mode.",
                actionText = "Allow Exact Alarms",
                settingsAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                } else null
            ),
            PermissionState(
                isGranted = hasNotificationPermission(context),
                isRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                title = "Notification Permission",
                description = "This allows SleepWell to show alarm notifications and service status.",
                actionText = "Allow Notifications",
                settingsAction = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            )
        )

        val allGranted = permissions.filter { it.isRequired }.all { it.isGranted }

        _uiState.value = _uiState.value.copy(
            permissions = permissions,
            allPermissionsGranted = allGranted
        )
    }

    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        val maxSteps = _uiState.value.permissions.size
        if (currentStep < maxSteps) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep + 1)
        }
    }

    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep - 1)
        }
    }

    fun openPermissionSettings(context: Context, permission: PermissionState) {
        try {
            val intent = when (permission.settingsAction) {
                Settings.ACTION_USAGE_ACCESS_SETTINGS -> {
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                }
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION -> {
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                }
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")
                        )
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                }
                Settings.ACTION_APP_NOTIFICATION_SETTINGS -> {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                }
                else -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to app settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }
}