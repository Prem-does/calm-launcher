package com.calmlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.policy.ModeEngine
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Best-effort in-scope enforcement for Focus Mode / Real Focus Sessions / Environment
 * blocking **and app limits**. When a blocked app comes to the foreground we bounce the user
 * back to the launcher with GLOBAL_ACTION_HOME. This is the strongest a non-device-owner
 * launcher can do; see [PlatformGuardPolicy] for the hard limits.
 *
 * App limits matter here because the launch gate only sees opens that start inside Calm. An
 * app reached from a notification, recents or another app would otherwise sail past a limit
 * the user has set.
 */
@AndroidEntryPoint
class FocusBlockAccessibilityService : AccessibilityService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var appLimitRepository: AppLimitRepository
    @Inject lateinit var modeEngine: ModeEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var settings: LauncherSettings = LauncherSettings()
    @Volatile private var categories: Map<String, AppCategory> = emptyMap()
    @Volatile private var labels: Map<String, String> = emptyMap()

    /** Packages with an enabled limit rule — the cheap in-memory pre-filter. */
    @Volatile private var limitedPackages: Map<String, AppLimitRule> = emptyMap()

    /** Last time each package was checked against its limit, to throttle usage queries. */
    private val lastLimitCheck = HashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository.settings
            .onEach { settings = it }
            .launchIn(scope)
        appRepository.observeApps()
            .onEach { apps ->
                categories = apps.associate { it.packageName to it.category }
                labels = apps.associate { it.packageName to it.label }
            }
            .launchIn(scope)
        appLimitRepository.observeRules()
            .onEach { rules -> limitedPackages = rules.filter { it.enabled }.associateBy { it.packageName } }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // never bounce ourselves

        val category = categories[pkg]
        if (category != null && modeEngine.isFocusBlocked(pkg, category, settings)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        enforceAppLimit(pkg)
    }

    /**
     * Check a foregrounded app against its daily limit. Reading real usage means a
     * UsageStatsManager query, so it is gated twice: the package must have an enabled rule,
     * and it must not have been checked within [LIMIT_CHECK_THROTTLE_MS].
     */
    private fun enforceAppLimit(pkg: String) {
        val rule = limitedPackages[pkg] ?: return
        val now = System.currentTimeMillis()
        if (rule.overrideUntilEpochMs > now) return
        synchronized(lastLimitCheck) {
            val last = lastLimitCheck[pkg] ?: 0L
            if (now - last < LIMIT_CHECK_THROTTLE_MS) return
            lastLimitCheck[pkg] = now
        }

        scope.launch {
            val label = labels[pkg] ?: pkg
            val decision = runCatching { appLimitRepository.evaluate(pkg, label) }.getOrNull()
            if (decision is AppLimitDecision.Blocked) {
                // performGlobalAction must be called from the service; hop back to main.
                withContext(Dispatchers.Main) { performGlobalAction(GLOBAL_ACTION_HOME) }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        scope.cancel()
        return super.onUnbind(intent)
    }

    private companion object {
        /** Don't re-query usage for the same app more than once every 20 seconds. */
        const val LIMIT_CHECK_THROTTLE_MS = 20_000L
    }
}
