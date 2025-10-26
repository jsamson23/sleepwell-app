package com.example.sleepwell.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.provider.Settings

object PermissionUtils {

    fun hasRequiredPermissions(context: Context): Boolean {
        return hasUsageStatsPermission(context) && hasOverlayPermission(context)
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
}