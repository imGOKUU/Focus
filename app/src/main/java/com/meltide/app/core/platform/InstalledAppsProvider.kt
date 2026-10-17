package com.meltide.app.core.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

/**
 * Resolving ACTION_MAIN/CATEGORY_LAUNCHER activities is exempt from Android
 * 11+ package-visibility filtering, so this works with no <queries> manifest
 * entry and no QUERY_ALL_PACKAGES permission.
 */
interface InstalledAppsProvider {
    fun getLaunchableApps(): List<InstalledApp>
}

@Singleton
class SystemInstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : InstalledAppsProvider {
    override fun getLaunchableApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .distinctBy { it.activityInfo.packageName }
            .filterNot { it.activityInfo.packageName == context.packageName }
            .map { resolveInfo ->
                InstalledApp(
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
}