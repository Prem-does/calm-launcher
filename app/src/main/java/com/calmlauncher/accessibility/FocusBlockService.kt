package com.calmlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.calmlauncher.launcher.lockQuickSettings
import com.calmlauncher.security.DevicePolicyEnforcer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FocusBlockService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastExternalPackage: String? = null
    private var lastScrollAtMillis = 0L
    private var lastScrollY = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentEvent = event ?: return
        val packageName = currentEvent.packageName?.toString().orEmpty()
        if (packageName.isBlank()) return

        when (currentEvent.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> inspectForegroundPackage(packageName)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> inspectScrollVelocity(currentEvent, packageName)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun inspectForegroundPackage(packageName: String) {
        serviceScope.launch {
            val settings = PlatformGuardPolicy.settings(this@FocusBlockService)
            val isSystemShade = packageName == "com.android.systemui"
            if (isSystemShade && settings.lockQuickSettings()) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                return@launch
            }

            val decision = PlatformGuardPolicy.decide(
                context = this@FocusBlockService,
                packageName = packageName,
                settings = settings,
                previousExternalPackage = lastExternalPackage
            )
            if (decision.shouldReturnHome) {
                // Try to close the previously-open app where possible (device-owner / best-effort).
                runCatching {
                    DevicePolicyEnforcer.closePackage(this@FocusBlockService, lastExternalPackage)
                }
                performGlobalAction(GLOBAL_ACTION_HOME)
                PlatformGuardPolicy.openLauncher(this@FocusBlockService)
            } else if (packageName != this@FocusBlockService.packageName) {
                lastExternalPackage = packageName
            }
        }
    }

    private fun inspectScrollVelocity(event: AccessibilityEvent, packageName: String) {
        val now = System.currentTimeMillis()
        val scrollY = event.scrollY
        val elapsed = now - lastScrollAtMillis
        val distance = kotlin.math.abs(scrollY - lastScrollY)
        lastScrollAtMillis = now
        lastScrollY = scrollY
        if (elapsed <= 0L || elapsed > 900L || distance < 900) return

        serviceScope.launch {
            val settings = PlatformGuardPolicy.settings(this@FocusBlockService)
            if (PlatformGuardPolicy.shouldWatchScroll(settings) && packageName != this@FocusBlockService.packageName) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                PlatformGuardPolicy.openLauncher(this@FocusBlockService)
            }
        }
    }
}
