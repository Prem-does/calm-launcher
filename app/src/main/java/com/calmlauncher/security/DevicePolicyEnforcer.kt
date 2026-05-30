package com.calmlauncher.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over [DevicePolicyManager] for the optional hardening features: screen
 * lock and (where the launcher is device owner) lock-task / kiosk. All calls are
 * best-effort and degrade to no-ops without the relevant privilege.
 */
@Singleton
class DevicePolicyEnforcer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dpm: DevicePolicyManager? =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private val admin: ComponentName = ComponentName(context, CalmDeviceAdminReceiver::class.java)

    fun isAdminActive(): Boolean = dpm?.isAdminActive(admin) == true

    fun isDeviceOwner(): Boolean = dpm?.isDeviceOwnerApp(context.packageName) == true

    /** Intent to request the user activate device admin. */
    fun requestAdminIntent(explanation: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
        }

    fun lockNow(): Boolean = if (isAdminActive()) {
        runCatching { dpm?.lockNow() }.isSuccess
    } else {
        false
    }

    /**
     * Kiosk / lock-task. Only fully effective when the launcher is device owner; on
     * normal installs this is a no-op (returns false). The actual start/stop must be
     * called from the Activity (startLockTask/stopLockTask); this just reports support.
     */
    fun isLockTaskSupported(): Boolean = isDeviceOwner()
}
