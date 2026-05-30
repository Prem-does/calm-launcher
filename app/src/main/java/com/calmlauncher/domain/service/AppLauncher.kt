package com.calmlauncher.domain.service

import com.calmlauncher.domain.model.LauncherTool

/**
 * Actually opens apps. Implemented in the data/system layer with PackageManager +
 * Intents (Samsung-aware). Kept Android-free here so the domain and ViewModels can
 * depend on it without platform types.
 */
interface AppLauncher {
    fun canLaunch(packageName: String): Boolean
    fun launch(packageName: String): Boolean

    /** Resolve + open a built-in tool, preferring Samsung One UI packages. */
    fun launchTool(tool: LauncherTool): Boolean

    /** Package name a tool currently resolves to, or null if unavailable. */
    fun resolveToolPackage(tool: LauncherTool): String?
}

/**
 * Deep-links into the system surfaces the launcher needs the user to visit
 * (permissions, default-home role, battery exemption). Each returns true if it could
 * launch the relevant screen.
 */
interface SystemActions {
    fun openAppInfo(packageName: String): Boolean
    fun openAccessibilitySettings(): Boolean
    fun openUsageAccessSettings(): Boolean
    fun openNotificationListenerSettings(): Boolean
    fun openDefaultHomeSettings(): Boolean
    fun requestIgnoreBatteryOptimizations(): Boolean
    fun openBatteryOptimizationSettings(): Boolean
    fun lockScreen(): Boolean
}
