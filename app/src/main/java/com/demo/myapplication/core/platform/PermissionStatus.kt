package com.demo.myapplication.core.platform

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** One place for "is X allowed?" checks and "take me to X" intents. */
object PermissionStatus {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityOn = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
        if (!accessibilityOn) return false

        val expected = ComponentName(context, AppBlockAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /** Runtime permission on Android 13+, and the app-level switch on every version. */
    fun canPostNotifications(context: Context): Boolean =
        hasNotificationRuntimePermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Always true below Android 13, where there's no runtime prompt. */
    fun hasNotificationRuntimePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // The per-app notification screen only exists from Android 8; before that
    // the app's details page is where its notification switch lives.
    fun notificationSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
