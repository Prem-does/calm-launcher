package com.calmlauncher.data.system

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.calmlauncher.domain.service.SystemActions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deep-links into the system surfaces the launcher needs the user to visit. Every method
 * is wrapped in runCatching and returns whether the relevant screen/action could be
 * launched, so callers can surface a graceful "couldn't open settings" message.
 */
@Singleton
class SystemActionsImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SystemActions {

    override fun openAppInfo(packageName: String): Boolean = start(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        },
    )

    override fun openAccessibilitySettings(): Boolean =
        start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    override fun openUsageAccessSettings(): Boolean =
        start(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    override fun openNotificationListenerSettings(): Boolean =
        start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    override fun openDefaultHomeSettings(): Boolean =
        start(Intent(Settings.ACTION_HOME_SETTINGS))

    @android.annotation.SuppressLint("BatteryLife")
    override fun requestIgnoreBatteryOptimizations(): Boolean = start(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        },
    )

    override fun openBatteryOptimizationSettings(): Boolean =
        start(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    /**
     * Locks the screen via [DevicePolicyManager] when this app holds an active device
     * admin. Without admin there is no supported way for a launcher to lock the device,
     * so we return false rather than throwing.
     */
    override fun lockScreen(): Boolean = runCatching {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        if (!hasActiveAdmin(dpm)) return false
        dpm.lockNow()
        true
    }.getOrDefault(false)

    private fun hasActiveAdmin(dpm: DevicePolicyManager): Boolean = runCatching {
        val admins: List<ComponentName> = dpm.activeAdmins ?: return false
        admins.any { it.packageName == context.packageName }
    }.getOrDefault(false)

    private fun start(intent: Intent): Boolean = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
