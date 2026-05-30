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
            (distracting || settings.frictionLevel >= FrictionLevel.MONK || analog)
        ) {
            steps += FrictionStep.Reason
        }

        // ---- 4. Breath Unlock -----------------------------------------------------------
        if (settings.breathUnlockEnabled) {
            steps += FrictionStep.Breath(FrictionRules.breathCycles(settings.frictionLevel))
        }

        // ---- 5. Opening Delay / Slow Mode / friction tier -------------------------------
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

        // ---- 6. Confirmation (HARDCORE only, distracting apps) --------------------------
        if (settings.frictionLevel == FrictionLevel.HARDCORE && distracting) {
            steps += FrictionStep.Confirm("Open ${request.label} anyway?")
        }

        // ---- 7. Fast path: nothing to do → open immediately -----------------------------
        // (An empty step list already equals LaunchDecision.Allow; returned for clarity.)
        return if (steps.isEmpty()) LaunchDecision.Allow else LaunchDecision(steps)
    }

    override fun restrictionState(
        settings: LauncherSettings,
        risk: RiskState,
    ): UiRestrictionState {
        val tier = risk.tier
        val atElevatedOrAbove = tier >= RiskTier.ELEVATED

        // Grayscale: user toggle, or forced when usage looks high/needs recovery.
        val grayscale = settings.grayscaleEnabled || tier == RiskTier.HIGH || tier == RiskTier.RECOVERY
        val grayscaleAmount = when {
            tier == RiskTier.RECOVERY -> 1f          // fully drained in Recovery Mode
            tier == RiskTier.HIGH -> 0.6f            // mostly desaturated when risk is HIGH
            settings.grayscaleEnabled -> 1f          // user explicitly asked for grayscale
            else -> 0f
        }

        // Dynamic Minimalism only escalates when the user has opted into it.
        val dynamicMinimalismActive = settings.dynamicMinimalismEnabled
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

        // Hide suggestions/recents per settings, and additionally as risk climbs.
        val hideSuggestions = !settings.showSuggestions || (dynamicMinimalismActive && atElevatedOrAbove)
        val hideRecents = !settings.showRecents || atElevatedOrAbove

        // E-ink simulation kills motion; Recovery also stills the interface.
        val motionEnabled = !settings.einkSimulationEnabled && tier != RiskTier.RECOVERY

        val forceFocusSuggested = settings.recoveryModeEnabled && tier == RiskTier.RECOVERY

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
