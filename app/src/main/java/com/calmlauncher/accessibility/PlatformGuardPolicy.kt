package com.calmlauncher.accessibility

import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils

/**
 * Central, honest record of which restrictions the launcher can enforce with normal +
 * accessibility + device-admin permissions, and which genuinely require OS-level
 * privileges (device-owner / WRITE_SECURE_SETTINGS) that a sideloaded launcher cannot
 * obtain. The Settings/Onboarding surfaces use the runtime checks below to show status.
 *
 * Hard limits (documented, not silently pretended):
 *  - True OS-level app killing for One-App-At-A-Time (needs device-owner / usage-kill).
 *  - Global grayscale/dim enforcement (needs WRITE_SECURE_SETTINGS or device-owner).
 *  - Blocking quick settings / installs / notifications outside launcher scope.
 *  - Detecting scroll velocity inside third-party apps.
 * Everything else is enforced best-effort via the accessibility focus guard, the
 * notification listener, in-launcher grayscale, and device-admin lock.
 */
object PlatformGuardPolicy {

    fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expected = ComponentName(context, service).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        while (splitter.hasNext()) {
            val component = splitter.next()
            if (component.equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return flat.contains(context.packageName)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE)
                as android.app.AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (t: Throwable) {
            false
        }
    }
}
