package com.calmlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Placeholder accessibility service for the grayscale feature.
 *
 * IMPORTANT / honest limitation: an accessibility service cannot desaturate the whole
 * screen. True system-wide grayscale is the secure setting
 * `accessibility_display_daltonizer_enabled` which requires WRITE_SECURE_SETTINGS or
 * device-owner — privileges a sideloaded launcher cannot hold. The launcher therefore
 * enforces grayscale *inside its own UI* via `Modifier.grayscale` and exposes this
 * service only so the user can wire up stronger enforcement if they grant the secure
 * setting via ADB. See [PlatformGuardPolicy].
 */
class GrayscaleAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
