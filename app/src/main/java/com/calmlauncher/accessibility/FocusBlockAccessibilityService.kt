package com.calmlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.model.OverrideResult
import com.calmlauncher.domain.policy.ModeEngine
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.domain.service.AppLauncher
import com.calmlauncher.launcher.LauncherActivity
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * In-scope enforcement for Focus Mode / Real Focus Sessions / Environment blocking **and app
 * limits**, covering apps that were never opened through Calm — reached from a notification, from
 * recents, or from another app.
 *
 * When a blocked app comes to the foreground it is covered by a full-screen
 * [BlockOverlayController] that says what happened, and the user is returned to the launcher once
 * it has had its say. The overlay is the point: an instant, silent `GLOBAL_ACTION_HOME` ejects
 * people mid-sentence with no idea why, which reads as a crash rather than as a boundary they set
 * for themselves.
 *
 * ## Why blocking used to be unreliable
 *
 * The old version asked "is this app over its limit?" on *every* foreground event, and that
 * question is expensive — it means a `UsageStatsManager` query — so it was throttled to once per
 * app per 20 seconds. The throttle was the hole. It applied to the *decision*, not just the
 * lookup, so once an app had been checked the answer was thrown away, and re-entering the app
 * inside the throttle window skipped the check entirely. Switching away and back, or bouncing
 * through recents, bought 20 seconds at a time indefinitely.
 *
 * Now the *verdict* is cached instead of the check being suppressed ([blockedUntil]):
 *
 *  - A package known to be blocked is re-blocked **immediately**, with no usage query and no
 *    throttle. Rapid switching therefore makes blocking faster, not slower.
 *  - The throttle now only guards the negative case — an app that was under its limit last time we
 *    looked — which is the only case where a stale answer is harmless.
 *  - Cached verdicts are invalidated when a rule changes or an override is granted, so one can
 *    never outlive the thing it describes.
 *
 * ## Split-screen, PiP, and recents
 *
 * `TYPE_WINDOW_STATE_CHANGED` reports one package, which is not enough when two apps are visible
 * at once: in split-screen the blocked app can sit in the half the user isn't touching and never
 * generate another event. [enforceVisibleWindows] therefore walks the full window list on every
 * event and blocks any visible window belonging to a known-blocked package. Picture-in-picture is
 * handled the same way, with the addition of [AppLauncher.closeApp] — a PiP window survives
 * `GLOBAL_ACTION_HOME`, so going home is not enough to end it.
 *
 * See [PlatformGuardPolicy] for what a non-device-owner launcher fundamentally cannot do.
 */
@AndroidEntryPoint
class FocusBlockAccessibilityService : AccessibilityService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var appLimitRepository: AppLimitRepository
    @Inject lateinit var modeEngine: ModeEngine
    @Inject lateinit var blockOverlay: BlockOverlayController
    @Inject lateinit var appLauncher: AppLauncher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var settings: LauncherSettings = LauncherSettings()
    @Volatile private var categories: Map<String, AppCategory> = emptyMap()
    @Volatile private var labels: Map<String, String> = emptyMap()

    /** Packages with an enabled limit rule — the cheap in-memory pre-filter. */
    @Volatile private var limitedPackages: Map<String, AppLimitRule> = emptyMap()

    /**
     * Packages known to be over their limit, and the instant that verdict expires.
     *
     * This is what makes blocking instant on re-entry. A hit here needs no database read and no
     * usage query, so it keeps up with the user switching apps as fast as they can.
     */
    private val blockedUntil = ConcurrentHashMap<String, Long>()

    /** The status behind a cached block, so the overlay can be rebuilt without re-querying. */
    private val blockedStatus = ConcurrentHashMap<String, AppLimitStatus>()

    /** Last known-*good* check per package, throttling only the expensive negative path. */
    private val lastCleanCheck = ConcurrentHashMap<String, Long>()

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
            .onEach { rules ->
                limitedPackages = rules.filter { it.enabled }.associateBy { it.packageName }
                invalidateVerdicts(rules)
            }
            .launchIn(scope)
    }

    /**
     * Drop cached verdicts a rule change has invalidated.
     *
     * An app inside a live override window is un-blocked here and now — the user paid for that time
     * and must not be bounced out of it. A disabled rule likewise: enforcing a remembered verdict
     * after the limit was switched off is the mirror image of the bug this cache exists to fix.
     *
     * The deleted case is easy to miss and matters just as much. A removed rule doesn't appear in
     * [rules] at all, so iterating the list can never clear it — the cached verdict would outlive
     * the rule and keep an app blocked until midnight after its limit was deleted. Hence the sweep
     * over cached packages that are no longer represented.
     */
    private fun invalidateVerdicts(rules: List<AppLimitRule>) {
        val now = System.currentTimeMillis()
        val stale = mutableSetOf<String>()

        rules.forEach { rule ->
            val overridden = rule.overrideUntilEpochMs > now
            if (!rule.enabled || overridden) stale += rule.packageName
        }

        // Anything cached that no longer has an enabled rule at all — deleted, or dropped from a
        // group — is no longer ours to block.
        val enabled = rules.filter { it.enabled }.map { it.packageName }.toSet()
        stale += blockedUntil.keys.filterNot { it in enabled }

        stale.forEach { pkg ->
            blockedUntil.remove(pkg)
            blockedStatus.remove(pkg)
            lastCleanCheck.remove(pkg)
            if (blockOverlay.showingPackage() == pkg) blockOverlay.hide()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val received = event ?: return
        when (received.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            -> Unit

            else -> return
        }

        // Sweep every visible window first. In split-screen, or with a PiP window up, the package
        // named in the event is only one of the things on screen and the blocked one may not be it.
        if (enforceVisibleWindows()) return

        val pkg = received.packageName?.toString() ?: return
        if (pkg == packageName) return // never bounce ourselves

        // The user has moved on while an overlay was up (they hit home, or the app closed itself).
        // Don't leave a black screen floating over something innocent.
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
     * Block any visible window whose package is already known to be over its limit, returning true
     * when something was blocked.
     *
     * This is what covers split-screen and picture-in-picture. Those windows are visible and
     * interactive without ever being the subject of a `TYPE_WINDOW_STATE_CHANGED` event, so waiting
     * to be told about them means never blocking them.
     */
    private fun enforceVisibleWindows(): Boolean {
        if (blockedUntil.isEmpty()) return false
        val visible = runCatching {
            windows.orEmpty()
                .filter { it.isVisibleAppWindow() }
                .mapNotNull { window -> window.root?.packageName?.toString() }
        }.getOrDefault(emptyList())

        val offender = visible.firstOrNull { it != packageName && isCachedBlocked(it) }
            ?: return false
        val status = blockedStatus[offender] ?: return false
        showLimitBlock(status)
        return true
    }

    /** Visible, interactive app windows only — not the status bar, the IME, or our own overlay. */
    private fun AccessibilityWindowInfo.isVisibleAppWindow(): Boolean = runCatching {
        type == AccessibilityWindowInfo.TYPE_APPLICATION
    }.getOrDefault(false)

    /** True while [pkg]'s cached "over the limit" verdict is still valid. Self-expiring. */
    private fun isCachedBlocked(pkg: String): Boolean {
        val until = blockedUntil[pkg] ?: return false
        if (System.currentTimeMillis() >= until) {
            blockedUntil.remove(pkg)
            blockedStatus.remove(pkg)
            return false
        }
        return true
    }

    /**
     * A focus session or environment preset refused this app. There is no override to offer, so the
     * overlay is a brief explanation and nothing more.
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
            onExit = { goHome(pkg) },
        )
        if (!shown) goHome(pkg)
    }

    /**
     * Check a foregrounded app against its daily limit.
     *
     * A cached verdict short-circuits everything, and that path is deliberately free of both the
     * throttle and the database — it is the path taken when someone is switching apps quickly to
     * get back into one that is blocked.
     */
    private fun enforceAppLimit(pkg: String) {
        // Known blocked: re-assert at once. No query, no throttle, no window to slip through.
        if (isCachedBlocked(pkg)) {
            blockedStatus[pkg]?.let { showLimitBlock(it) }
            return
        }

        val rule = limitedPackages[pkg] ?: return
        val now = System.currentTimeMillis()
        if (rule.overrideUntilEpochMs > now) return
        if (blockOverlay.showingPackage() == pkg) return

        // The throttle now only covers apps that were *under* their limit when last checked. A
        // stale "still fine" is harmless for a few seconds; a stale "already blocked" was the bug.
        val lastClean = lastCleanCheck[pkg] ?: 0L
        if (now - lastClean < CLEAN_CHECK_THROTTLE_MS) return

        scope.launch {
            val label = labels[pkg] ?: pkg
            val decision = runCatching { appLimitRepository.evaluate(pkg, label) }.getOrNull()
            if (decision is AppLimitDecision.Blocked) {
                cacheBlock(pkg, decision.status)
                // Overlay and performGlobalAction both belong to the main thread.
                withContext(Dispatchers.Main) { showLimitBlock(decision.status) }
            } else {
                lastCleanCheck[pkg] = System.currentTimeMillis()
            }
        }
    }

    /**
     * Remember that [pkg] is blocked, until the daily reset or the end of an override window.
     *
     * Bounding the cache at the reset is what lets the verdict be trusted without re-checking:
     * usage only climbs, so an app over its limit stays over it until the limit itself resets.
     */
    private fun cacheBlock(pkg: String, status: AppLimitStatus) {
        blockedUntil[pkg] = nextDayReset()
        blockedStatus[pkg] = status
        lastCleanCheck.remove(pkg)
    }

    /** The "your time is up" screen, offering the extension only if any are left today. */
    private fun showLimitBlock(status: AppLimitStatus) {
        val pkg = status.packageName
        if (blockOverlay.showingPackage() == pkg) return
        val canExtend = status.canGrantOverride
        val extensionMinutes = extensionMinutesFor(status)
        val shown = blockOverlay.show(
            spec = BlockOverlaySpec(
                packageName = pkg,
                title = "Limit reached",
                appLabel = status.label,
                detail = "${status.usedMinutes}m used today of ${status.dailyLimitMinutes ?: 0}m.",
                countdownSeconds = LIMIT_COUNTDOWN_SECONDS,
                overrideLabel = if (canExtend) "Add $extensionMinutes minutes" else null,
                // With no extension to weigh up there is nothing to decide, so the exit follows the
                // countdown immediately.
                graceSeconds = if (canExtend) OVERRIDE_GRACE_SECONDS else 0,
                footnote = status.overrideExhaustedReason,
            ),
            onOverride = if (canExtend) {
                { onResult ->
                    scope.launch {
                        val result = runCatching {
                            appLimitRepository.extendOverride(pkg, extensionMinutes)
                        }.getOrNull()

                        when (result) {
                            is OverrideResult.Granted -> {
                                // The user paid for this window: stop treating the app as blocked
                                // until it expires, and leave them where they are.
                                blockedUntil.remove(pkg)
                                blockedStatus.remove(pkg)
                                lastCleanCheck[pkg] = System.currentTimeMillis()
                                onResult(true)
                            }

                            // Refused, or the call threw. Either way the block stands. Reporting
                            // false is what stops the overlay dismissing itself and handing over
                            // free time, which was the original exploit.
                            else -> onResult(false)
                        }
                    }
                }
            } else {
                null
            },
            onExit = { goHome(pkg) },
        )
        if (!shown) goHome(pkg)
    }

    /** A single extension is worth the standard amount, capped by what's left in the budget. */
    private fun extensionMinutesFor(status: AppLimitStatus): Int =
        minOf(DEFAULT_EXTENSION_MINUTES, status.overrideMinutesRemaining).coerceAtLeast(1)

    /**
     * Get the user out of [pkg] and back to the launcher, and make it stick.
     *
     * Three actions rather than one, because `GLOBAL_ACTION_HOME` alone is not enough:
     *  - it does nothing about a picture-in-picture window, which keeps playing over the home
     *    screen, so the app is also asked to stop;
     *  - some OEM home implementations swallow the global action, so the launcher is started
     *    explicitly as well;
     *  - a blocked app left at the top of the recents stack is one gesture from being resumed, and
     *    starting the launcher with `CLEAR_TOP` is what takes that slot back.
     *
     * All best-effort: without device-owner status the system may decline any of them, which is why
     * there are three and not one.
     */
    private fun goHome(pkg: String? = null) {
        runCatching { performGlobalAction(GLOBAL_ACTION_HOME) }
        runCatching {
            startActivity(
                Intent(this, LauncherActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION,
                ),
            )
        }
        if (pkg != null && pkg != packageName) {
            scope.launch { runCatching { appLauncher.closeApp(pkg) } }
        }
    }

    /** Next local midnight, matching the day boundary the limit repository uses. */
    private fun nextDayReset(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        blockOverlay.hide()
        blockedUntil.clear()
        blockedStatus.clear()
        lastCleanCheck.clear()
        scope.cancel()
        return super.onUnbind(intent)
    }

    private companion object {
        /**
         * Don't re-query usage for an app that was *under* its limit more than once every few
         * seconds. Deliberately much shorter than the 20s it replaced: this now only delays
         * noticing that an app has just crossed its limit, and never delays re-blocking one that
         * already has.
         */
        const val CLEAN_CHECK_THROTTLE_MS = 5_000L

        /** Beat to sit with an exhausted limit before the actions appear. */
        const val LIMIT_COUNTDOWN_SECONDS = 10

        /** Shorter pause for a focus block — there is no decision to make. */
        const val FOCUS_COUNTDOWN_SECONDS = 5

        /** How long the extension stays on offer before the user is sent home anyway. */
        const val OVERRIDE_GRACE_SECONDS = 8

        const val DEFAULT_EXTENSION_MINUTES = 10
    }
}
