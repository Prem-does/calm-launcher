package com.calmlauncher.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.calmlauncher.launcher.LauncherSettingsState

class CalmDeviceAdminReceiver : DeviceAdminReceiver() {
	override fun onEnabled(context: Context, intent: Intent) {
		// Re-apply device policies when admin is enabled.
		try {
			DevicePolicyEnforcer.apply(context, LauncherSettingsState())
		} catch (_: Throwable) {
		}
		super.onEnabled(context, intent)
	}

	override fun onDisabled(context: Context, intent: Intent) {
		// Clear or re-evaluate policies when admin is disabled.
		try {
			DevicePolicyEnforcer.apply(context, LauncherSettingsState())
		} catch (_: Throwable) {
		}
		super.onDisabled(context, intent)
	}
}
