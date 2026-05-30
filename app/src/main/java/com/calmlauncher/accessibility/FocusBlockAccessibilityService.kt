package com.calmlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.policy.ModeEngine
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Best-effort in-scope enforcement for Focus Mode / Real Focus Sessions / Environment
 * blocking. When a blocked app comes to the foreground we bounce the user back to the
 * launcher with GLOBAL_ACTION_HOME. This is the strongest a non-device-owner launcher
 * can do; see [PlatformGuardPolicy] for the hard limits.
 */
@AndroidEntryPoint
class FocusBlockAccessibilityService : AccessibilityService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var modeEngine: ModeEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var settings: LauncherSettings = LauncherSettings()
    @Volatile private var categories: Map<String, AppCategory> = emptyMap()

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository.settings
            .onEach { settings = it }
            .launchIn(scope)
        appRepository.observeApps()
            .onEach { apps -> categories = apps.associate { it.packageName to it.category } }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // never bounce ourselves
        val category = categories[pkg] ?: return // unknown / system window — ignore
        if (modeEngine.isFocusBlocked(pkg, category, settings)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        scope.cancel()
        return super.onUnbind(intent)
    }
}
