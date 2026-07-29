package com.calmlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.policy.ModeEngine
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.overlay.BlockOverlayController
import com.calmlauncher.overlay.BlockOverlaySpec
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
 * In-scope enforcement for Focus Mode / Real Focus Sessions / Environment blocking **and app
 * limits**, covering apps that were never opened through Calm — reached from a notification,
 * from recents, or from another app.
 *
 * When a blocked app comes to the foreground it is covered by a full-screen
 * [BlockOverlayController] that says what happened, and the user is returned to the launcher
 * once it has had its say. The overlay is the point: the previous behaviour was an instant,
 * silent GLOBAL_ACTION_HOME that ejected people mid-sentence with no idea why, which reads as
 * a crash rather than as a boundary they set for themselves. An exhausted limit also offers
 * the same extension the in-launcher gate would, so this path is no stricter than the front
 * door.
 *
 * The plain bounce remains the fallback whenever the overlay can't be drawn ("display over
 * other apps" is an optional grant), because an unexplained limit still beats an unenforced
 * one. See [PlatformGuardPolicy] for what a non-device-owner launcher fundamentally cannot do.
 */
@AndroidEntryPoint
class FocusBlockAccessibilityService : AccessibilityService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var appLimitRepository: AppLimitRepository
    @Inject lateinit var modeEngine: ModeEngine
    @Inject lateinit var blockOverlay: BlockOverlayController

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

        // The user has moved on while an overlay was up (they hit home, or the app closed
        // itself). Don't leave a black screen floating over something innocent.
        blockOverlay.showingPackage()?.let { covered ->
            if (covered != pkg) blockOverlay.hide()
        }

        val category = categories[pkg]
        if (category != null && modeEngine.isFocusBlocked(pkg, category, settings)) {
            showFocusBlock(pkg)
            return
        }

        enforceAppLimit(pkg)
    }

    /**
     * A focus session or environment preset refused this app. There is no override to offer,
     * so the overlay is a brief explanation and nothing more.
     */
    private fun showFocusBlock(pkg: String) {
        if (blockOverlay.showingPackage() == pkg) return
        val label = labels[pkg] ?: pkg
        val shown = blockOverlay.show(
            spec = BlockOverlaySpec(
                packageName = pkg,
                title = "Blocked",
                appLabel = label,
                detail = "Focus mode is on. This one is off limits right now.",
                countdownSeconds = FOCUS_COUNTDOWN_SECONDS,
            ),
            onExit = { goHome() },
        )
        if (!shown) goHome()
    }

    /**
     * Check a foregrounded app against its daily limit. Reading real usage means a
     * UsageStatsManager query, so it is gated three ways: the package must have an enabled
     * rule, it must not already be covered, and it must not have been checked within
     * [LIMIT_CHECK_THROTTLE_MS].
     */
    private fun enforceAppLimit(pkg: String) {
        val rule = limitedPackages[pkg] ?: return
        val now = System.currentTimeMillis()
        if (rule.overrideUntilEpochMs > now) return
        if (blockOverlay.showingPackage() == pkg) return
        synchronized(lastLimitCheck) {
            val last = lastLimitCheck[pkg] ?: 0L
            if (now - last < LIMIT_CHECK_THROTTLE_MS) return
            lastLimitCheck[pkg] = now
        }

        scope.launch {
            val label = labels[pkg] ?: pkg
            val decision = runCatching { appLimitRepository.evaluate(pkg, label) }.getOrNull()
            if (decision is AppLimitDecision.Blocked) {
                // Overlay and performGlobalAction both belong to the main thread.
                withContext(Dispatchers.Main) { showLimitBlock(decision.status) }
            }
        }
    }

    /** The "your time is up" screen, offering the extension only if any are left today. */
    private fun showLimitBlock(status: AppLimitStatus) {
        val canExtend = status.canGrantOverride
        val shown = blockOverlay.show(
            spec = BlockOverlaySpec(
                packageName = status.packageName,
                title = "Limit reached",
                appLabel = status.label,
                detail = "${status.usedMinutes}m used today of ${status.dailyLimitMinutes ?: 0}m.",
                countdownSeconds = LIMIT_COUNTDOWN_SECONDS,
                overrideLabel = if (canExtend) "Add $OVERRIDE_MINUTES minutes" else null,
                // With no extension to weigh up there is nothing to decide, so the exit follows
                // the countdown immediately.
                graceSeconds = if (canExtend) OVERRIDE_GRACE_SECONDS else 0,
                footnote = if (canExtend) null else "Both extensions are used up for today.",
            ),
            onOverride = if (canExtend) {
                {
                    scope.launch {
                        runCatching {
                            appLimitRepository.extendOverride(status.packageName, OVERRIDE_MINUTES)
                        }
                        // Don't re-block on the next window event while the write settles.
                        synchronized(lastLimitCheck) {
                            lastLimitCheck[status.packageName] = System.currentTimeMillis()
                        }
                    }
                }
            } else {
                null
            },
            onExit = { goHome() },
        )
        if (!shown) goHome()
    }

    /** performGlobalAction must be called from the service itself. */
    private fun goHome() {
        runCatching { performGlobalAction(GLOBAL_ACTION_HOME) }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        blockOverlay.hide()
        scope.cancel()
        return super.onUnbind(intent)
    }

    private companion object {
        /** Don't re-query usage for the same app more than once every 20 seconds. */
        const val LIMIT_CHECK_THROTTLE_MS = 20_000L

        /** Beat to sit with an exhausted limit before the actions appear. */
        const val LIMIT_COUNTDOWN_SECONDS = 10

        /** Shorter pause for a focus block — there is no decision to make. */
        const val FOCUS_COUNTDOWN_SECONDS = 5

        /** How long the extension stays on offer before the user is sent home anyway. */
        const val OVERRIDE_GRACE_SECONDS = 8

        const val OVERRIDE_MINUTES = 10
    }
}
