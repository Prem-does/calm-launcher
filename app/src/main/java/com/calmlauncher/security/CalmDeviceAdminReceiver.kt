package com.calmlauncher.security

import android.app.admin.DeviceAdminReceiver

/**
 * Device-admin receiver. Enables the launcher to lock the screen (and, on devices where
 * the launcher is a device owner, stronger restrictions). Activated by the user from
 * Settings; never required for the core experience.
 */
class CalmDeviceAdminReceiver : DeviceAdminReceiver()
