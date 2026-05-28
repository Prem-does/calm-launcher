package com.calmlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.calmlauncher.launcher.grayscaleModeChoice
import com.calmlauncher.launcher.useExtraDim
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GreyscaleAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dimOverlay: View? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        refreshOverlay()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshOverlay()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        removeOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun refreshOverlay() {
        serviceScope.launch {
            val settings = PlatformGuardPolicy.settings(this@GreyscaleAccessibilityService)
            val shouldDim = settings.grayscaleForced || settings.useExtraDim() || settings.grayscaleModeChoice() != "Off"
            withContext(Dispatchers.Main) {
                if (shouldDim) {
                    showDimOverlay()
                } else {
                    removeOverlay()
                }
            }
        }
    }

    private fun showDimOverlay() {
        if (dimOverlay != null) return
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val overlay = View(this).apply {
            setBackgroundColor(0x44000000)
            isClickable = false
            isFocusable = false
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        runCatching {
            windowManager.addView(overlay, params)
            dimOverlay = overlay
        }
    }

    private fun removeOverlay() {
        val overlay = dimOverlay ?: return
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        runCatching { windowManager.removeView(overlay) }
        dimOverlay = null
    }
}
