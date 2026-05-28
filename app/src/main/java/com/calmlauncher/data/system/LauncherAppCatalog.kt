package com.calmlauncher.data.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.allowedAppsListEnabled
import com.calmlauncher.launcher.filterAppItems
import com.calmlauncher.launcher.invisibleSocialApps
import com.calmlauncher.launcher.minimalSocialLayer
import com.calmlauncher.security.DevicePolicyEnforcer

data class LauncherAppItem(
    val label: String,
    val packageName: String,
    val launchIntent: Intent
)

object LauncherAppCatalog {
    private val allowedLabels = listOf(
        "Phone",
        "Messages",
        "Alarm",
        "Calculator",
        "Calendar",
        "Camera",
        "Maps",
        "Notes",
        "Music",
        "Settings"
    )

    fun loadAllowedApps(
        context: Context,
        settings: LauncherSettingsState = LauncherSettingsState(),
        focusModeActive: Boolean = settings.focusModeEnabled
    ): List<LauncherAppItem> {
        val items = loadAllLaunchableApps(context)
            .filter { item ->
                if (settings.allowedAppsListEnabled()) {
                    allowedLabels.any { allowed -> item.label.equals(allowed, ignoreCase = true) }
                } else {
                    true
                }
            }
        return settings.filterAppItems(items, focusModeActive)
            .sortedBy { item -> allowedLabels.indexOfFirst { it.equals(item.label, ignoreCase = true) }.let { if (it < 0) Int.MAX_VALUE else it } }
    }

    fun loadSearchApps(
        context: Context,
        settings: LauncherSettingsState = LauncherSettingsState()
    ): List<LauncherAppItem> {
        return loadAllLaunchableApps(context)
            .filter { item ->
                if (settings.allowedAppsListEnabled()) {
                    allowedLabels.any { allowed -> item.label.equals(allowed, ignoreCase = true) } ||
                        ((settings.invisibleSocialApps() || settings.minimalSocialLayer()) && item.isSocialApp())
                } else {
                    true
                }
            }
    }

    fun launchApp(context: Context, packageName: String) {
        // Best-effort: unsuspend package before launching if previously suspended by device-owner policies
        runCatching { DevicePolicyEnforcer.unsuspendPackage(context, packageName) }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun loadAllLaunchableApps(context: Context): List<LauncherAppItem> {
        val packageManager = context.packageManager
        val launchables = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )

        return launchables.mapNotNull { resolveInfo -> resolveInfo.toLauncherAppItem(packageManager) }
    }

    private fun ResolveInfo.toLauncherAppItem(packageManager: PackageManager): LauncherAppItem? {
        val activityInfo = activityInfo ?: return null
        val label = loadLabel(packageManager)?.toString()?.trim().orEmpty()
        if (label.isBlank()) return null

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(activityInfo.packageName, activityInfo.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return LauncherAppItem(
            label = label,
            packageName = activityInfo.packageName,
            launchIntent = intent
        )
    }

    private fun LauncherAppItem.isSocialApp(): Boolean {
        return listOf("instagram", "twitter", "x", "facebook", "threads", "snapchat", "reddit")
            .any { keyword -> label.contains(keyword, ignoreCase = true) }
    }
}
