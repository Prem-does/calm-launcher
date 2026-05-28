package com.calmlauncher.security

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.calmlauncher.data.db.CalmDatabaseProvider
import com.calmlauncher.data.db.entity.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.blockBrowser
import com.calmlauncher.launcher.blockAppInstallation
import com.calmlauncher.launcher.blockPlayStore
import com.calmlauncher.launcher.blockSocialMedia
import com.calmlauncher.launcher.disableEntertainmentApps
import com.calmlauncher.launcher.disableInternetAccess
import com.calmlauncher.launcher.kioskModeEnabledPref
import com.calmlauncher.launcher.lockQuickSettings
import com.calmlauncher.launcher.lockSafeModeAccess
import com.calmlauncher.launcher.preventLauncherExit

object DevicePolicyEnforcer {
    fun adminComponent(context: Context): ComponentName {
        return ComponentName(context, CalmDeviceAdminReceiver::class.java)
    }

    fun isAdminActive(context: Context): Boolean {
        val manager = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return manager.isAdminActive(adminComponent(context))
    }

    fun apply(context: Context, settings: LauncherSettingsState) {
        val manager = context.getSystemService(DevicePolicyManager::class.java) ?: return
        val admin = adminComponent(context)
        if (!manager.isAdminActive(admin)) return

        setRestriction(manager, admin, UserManager.DISALLOW_INSTALL_APPS, settings.blockAppInstallation())
        setRestriction(manager, admin, UserManager.DISALLOW_SAFE_BOOT, settings.lockSafeModeAccess())
        setRestriction(manager, admin, UserManager.DISALLOW_APPS_CONTROL, settings.preventLauncherExit())
        setRestriction(manager, admin, UserManager.DISALLOW_CONFIG_DATE_TIME, settings.preventLauncherExit())
        setStatusBarDisabled(manager, admin, settings.lockQuickSettings())

        if (settings.kioskModeEnabledPref() || settings.preventLauncherExit()) {
            runCatching {
                manager.setLockTaskPackages(admin, arrayOf(context.packageName))
            }
        }
        // Enforce package-level suspensions for browsers, social, stores, and entertainment when requested.
        runCatching { suspendDisallowedPackages(context, settings) }
    }

    /**
     * Attempt to close or suspend a running package. Works best when the launcher
     * is device-owner. This is best-effort: it tries DevicePolicyManager suspension
     * APIs first, then falls back to killing background processes.
     */
    fun closePackage(context: Context, packageName: String?) {
        if (packageName.isNullOrBlank()) return
        val manager = context.getSystemService(DevicePolicyManager::class.java) ?: return
        val admin = adminComponent(context)
        if (!manager.isAdminActive(admin)) return

        // Try to suspend the package via DevicePolicyManager (requires device-owner)
        runCatching {
            manager.setPackagesSuspended(admin, arrayOf(packageName), true)
        }

        // As a fallback, try to kill background processes (requires KILL_BACKGROUND_PROCESSES)
        runCatching {
            val activityManager = context.getSystemService(ActivityManager::class.java)
            activityManager?.killBackgroundProcesses(packageName)
        }
    }

    fun isDeviceOwner(context: Context): Boolean {
        val manager = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return runCatching { manager.isDeviceOwnerApp(context.packageName) }.getOrDefault(false)
    }

    /**
     * Suspend or unsuspend a package. Returns true on success.
     */
    fun setPackageSuspended(context: Context, packageName: String, suspended: Boolean): Boolean {
        val manager = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        val admin = adminComponent(context)
        if (!manager.isAdminActive(admin)) return false
        return runCatching {
            manager.setPackagesSuspended(admin, arrayOf(packageName), suspended)
            true
        }.getOrDefault(false)
    }

    fun unsuspendPackage(context: Context, packageName: String?) {
        if (packageName.isNullOrBlank()) return
        // Try unsuspend first
        runCatching {
            setPackageSuspended(context, packageName, false)
        }
        // No fallback needed for unsuspend; ensure background processes won't be killed erroneously.
    }

    private fun logTelemetry(context: Context, type: String, details: String) {
        try {
            val db = CalmDatabaseProvider.get(context)
            GlobalScope.launch(Dispatchers.IO) {
                db.telemetryDao().insert(TelemetryEvent(type = type, details = details, timestampMillis = System.currentTimeMillis()))
            }
        } catch (_: Throwable) {
        }
    }

    private fun suspendDisallowedPackages(context: Context, settings: LauncherSettingsState) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(0).map { it.packageName }

        val browserKeywords = listOf("chrome", "firefox", "browser", "brave", "edge")
        val storeKeywords = listOf("vending", "packageinstaller", "installer", "market", "play")
        val socialKeywords = listOf("instagram", "twitter", "facebook", "threads", "snapchat", "reddit", "tiktok")
        val entertainmentKeywords = listOf("youtube", "netflix", "spotify", "video", "music", "prime")

        fun matchesAny(pkg: String, keywords: List<String>): Boolean {
            return keywords.any { pkg.contains(it, ignoreCase = true) }
        }

        installed.forEach { pkg ->
            val shouldSuspend = when {
                settings.blockBrowser() && matchesAny(pkg, browserKeywords) -> true
                settings.blockPlayStore() && matchesAny(pkg, storeKeywords) -> true
                settings.blockSocialMedia() && matchesAny(pkg, socialKeywords) -> true
                settings.disableEntertainmentApps() && matchesAny(pkg, entertainmentKeywords) -> true
                settings.disableInternetAccess() && (matchesAny(pkg, browserKeywords) || matchesAny(pkg, socialKeywords) || matchesAny(pkg, entertainmentKeywords) || matchesAny(pkg, storeKeywords)) -> true
                else -> false
            }
            // Try to apply suspend/unsuspend only when admin is active.
            runCatching {
                val result = setPackageSuspended(context, pkg, shouldSuspend)
                if (result) {
                    logTelemetry(context, if (shouldSuspend) "package_suspended" else "package_unsuspended", pkg)
                }
            }
        }
    }

    private fun setRestriction(
        manager: DevicePolicyManager,
        admin: ComponentName,
        restriction: String,
        enabled: Boolean
    ) {
        runCatching {
            if (enabled) {
                manager.addUserRestriction(admin, restriction)
            } else {
                manager.clearUserRestriction(admin, restriction)
            }
        }
    }

    private fun setStatusBarDisabled(
        manager: DevicePolicyManager,
        admin: ComponentName,
        disabled: Boolean
    ) {
        runCatching { manager.setStatusBarDisabled(admin, disabled) }
    }
}
