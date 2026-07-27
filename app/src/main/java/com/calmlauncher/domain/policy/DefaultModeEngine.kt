package com.calmlauncher.domain.policy

import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.FrictionLevel
import com.calmlauncher.domain.model.FrictionStep
import com.calmlauncher.domain.model.LaunchDecision
import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.model.RiskState
import com.calmlauncher.domain.model.RiskTier
import com.calmlauncher.domain.model.UiRestrictionState
import javax.inject.Inject

/**
 * Default, pure implementation of [ModeEngine]. Composes every enabled anti-distraction
 * mode into an ordered [LaunchDecision] and derives the observable UI posture.
 *
 * NOTHING here touches I/O, Android, or the wall clock — the only notion of "now" is the
 * [nowEpochMs] passed into [resolve]. This keeps the brain of the app deterministic and
 * trivially unit-testable.
 */
class DefaultModeEngine @Inject constructor() : ModeEngine {

    override fun resolve(
        request: AppLaunchRequest,
        settings: LauncherSettings,
        risk: RiskState,
        recentEvents: List<LaunchEvent>,
        nowEpochMs: Long,
    ): LaunchDecision {
        val category = request.category
        val analog = settings.analogModeEnabled
        val distracting = FrictionRules.isDistracting(category, analog)

        // ---- 1. Focus rules / environment blocking (hard stop) --------------------------
        if (isFocusBlocked(request.packageName, category, settings)) {
            // Prefer a precise environment reason if the environment is what blocks it;
            // otherwise it's the explicit focus session refusing quick-exit apps.
            val reason =
                if (EnvironmentRules.blocks(settings.environmentMode, category, settings.focusActive)) {
                    EnvironmentRules.blockReason(settings.environmentMode)
                } else {
                    "In focus mode"
                }
            return LaunchDecision(listOf(FrictionStep.Block(reason)))
        }

        val steps = mutableListOf<FrictionStep>()

        // ---- 2. Dead-End Feeds (route to reset on compulsive re-opening) -----------------
        if (settings.deadEndFeedsEnabled && distracting &&
            openedAtLeastThriceRecently(request.packageName, recentEvents, nowEpochMs)
        ) {
            steps += FrictionStep.DeadEnd
            return LaunchDecision(steps)
        }

        // ---- 3. Intent prompt (Intent-Based App Opening) --------------------------------
        if (settings.intentPromptEnabled &&
            (distracting || settings.frictionLevel >= FrictionLevel.MEDIUM || analog)
        ) {
            steps += FrictionStep.Reason
        }

        // ---- 4. Opening Delay / Slow Mode / friction tier -------------------------------
        if (settings.openingDelaysEnabled) {
            val seconds = FrictionRules.delaySeconds(
                baseDelaySeconds = settings.defaultOpenDelaySeconds,
                level = settings.frictionLevel,
                slowMode = settings.slowModeEnabled,
                riskTier = risk.tier,
                isDistracting = distracting,
            )
            if (seconds > 0) steps += FrictionStep.Delay(seconds)
        }

        // ---- 5. Confirmation (HARDCORE only, distracting apps) --------------------------
        if (settings.frictionLevel == FrictionLevel.HARDCORE && distracting) {
            steps += FrictionStep.Confirm("Open ${request.label} anyway?")
        }

        // ---- 6. Fast path: nothing to do → open immediately -----------------------------
        // (An empty step list already equals LaunchDecision.Allow; returned for clarity.)
        return if (steps.isEmpty()) LaunchDecision.Allow else LaunchDecision(steps)
    }

    override fun restrictionState(
        settings: LauncherSettings,
        risk: RiskState,
    ): UiRestrictionState {
        val tier = risk.tier

        // Every risk-driven escalation below is gated on the toggle that owns it. Turning a
        // feature off in Settings must genuinely stop it — a rising risk tier is never allowed
        // to re-enable behaviour the user has switched off.
        val recoveryActive = settings.recoveryModeEnabled && tier == RiskTier.RECOVERY
        val dynamicMinimalismActive = settings.dynamicMinimalismEnabled
        val escalating = dynamicMinimalismActive && tier >= RiskTier.ELEVATED

        // Grayscale: the user toggle always wins. Dopamine Detection is what may additionally
        // drain colour as usage climbs, so it only happens while that feature is enabled.
        val autoGrayscale = settings.dopamineDetectionEnabled &&
            (tier == RiskTier.HIGH || recoveryActive)
        val grayscale = settings.grayscaleEnabled || autoGrayscale
        val grayscaleAmount = when {
            settings.grayscaleEnabled -> 1f                  // user explicitly asked for grayscale
            autoGrayscale && recoveryActive -> 1f            // fully drained in Recovery Mode
            autoGrayscale -> 0.6f                            // mostly desaturated when risk is HIGH
            else -> 0f
        }

        // Dynamic Minimalism only escalates when the user has opted into it.
        val minimalismLevel = if (dynamicMinimalismActive) {
            when (tier) {
                RiskTier.CALM -> 0
                RiskTier.ELEVATED -> 1
                RiskTier.HIGH -> 2
                RiskTier.RECOVERY -> 3
            }
        } else {
            0
        }

        // Hide suggestions/recents per settings, and additionally as risk climbs — but only
        // while Dynamic Minimalism is on. With it off, the plain Show Recents/Suggestions
        // preference is the only thing that decides.
        val hideSuggestions = !settings.showSuggestions || escalating
        val hideRecents = !settings.showRecents || escalating

        // E-ink simulation kills motion; Recovery also stills the interface when enabled.
        val motionEnabled = !settings.einkSimulationEnabled && !recoveryActive

        val forceFocusSuggested = recoveryActive

        return UiRestrictionState(
            grayscale = grayscale,
            grayscaleAmount = grayscaleAmount,
            hideSuggestions = hideSuggestions,
            hideRecents = hideRecents,
            minimalismLevel = minimalismLevel,
            motionEnabled = motionEnabled,
            forceFocusSuggested = forceFocusSuggested,
            riskTier = tier,
        )
    }

    override fun isFocusBlocked(
        packageName: String,
        category: AppCategory,
        settings: LauncherSettings,
    ): Boolean {
        // A real focus session blocks the classic "quick-exit" dopamine apps:
        // browsers, stores, social, entertainment and games.
        val focusSessionBlocks = settings.focusActive && category.isDistractingByDefault
        // Environment presets add their own blocks per the table in EnvironmentRules,
        // some of which apply even when no explicit focus session is running.
        val environmentBlocks =
            EnvironmentRules.blocks(settings.environmentMode, category, settings.focusActive)
        return focusSessionBlocks || environmentBlocks
    }

    /**
     * Dead-End trigger: the same package appears ≥ 3 times in [recentEvents] within the last
     * 30 minutes (compulsive re-opening). Events outside the window are ignored.
     */
    private fun openedAtLeastThriceRecently(
        packageName: String,
        recentEvents: List<LaunchEvent>,
        nowEpochMs: Long,
    ): Boolean {
        val windowStart = nowEpochMs - DEAD_END_WINDOW_MS
        var count = 0
        for (e in recentEvents) {
            if (e.packageName == packageName && e.timestampEpochMs >= windowStart && e.timestampEpochMs <= nowEpochMs) {
                count++
                if (count >= DEAD_END_MIN_OPENS) return true
            }
        }
        return false
    }

    private companion object {
        const val DEAD_END_WINDOW_MS = 30L * 60L * 1000L // 30 minutes
        const val DEAD_END_MIN_OPENS = 3
    }
}
