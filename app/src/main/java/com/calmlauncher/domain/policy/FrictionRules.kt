package com.calmlauncher.domain.policy

import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.FrictionLevel
import com.calmlauncher.domain.model.RiskTier

/**
 * Pure helpers for the friction-tier maths used by [DefaultModeEngine]. No I/O, no time,
 * no Android. All thresholds are documented inline so behaviour is easy to audit.
 */
internal object FrictionRules {

    /**
     * Whether an app should be treated as "distracting" for friction purposes. We honour the
     * category's [AppCategory.isDistractingByDefault] flag (SOCIAL / ENTERTAINMENT / BROWSER /
     * STORE / GAME). Analog mode makes *every* app feel deliberate, so it widens the net.
     */
    fun isDistracting(category: AppCategory, analogMode: Boolean): Boolean =
        analogMode || category.isDistractingByDefault

    /** Per-tier multiplier applied to the base opening delay: LIGHT×1, MEDIUM×2, HARDCORE×3. */
    fun delayMultiplier(level: FrictionLevel): Int = when (level) {
        FrictionLevel.LIGHT -> 1
        FrictionLevel.MEDIUM -> 2
        FrictionLevel.HARDCORE -> 3
    }

    /** Extra delay seconds contributed by the current risk tier (the Slow-Mode "tax"). */
    fun riskDelayBonus(tier: RiskTier): Int = when (tier) {
        RiskTier.CALM -> 0
        RiskTier.ELEVATED -> 1
        RiskTier.HIGH -> 2
        RiskTier.RECOVERY -> 3
    }

    /**
     * Compute the opening-delay seconds for a launch.
     *
     * Formula: `(baseDelay × tierMultiplier) + slowModeBonus + riskBonus`, where
     *  - baseDelay   = [baseDelaySeconds] (settings.defaultOpenDelaySeconds, floored at 0),
     *  - tierMult    = [delayMultiplier],
     *  - slowMode    = +2 when Slow Mode is on,
     *  - riskBonus   = [riskDelayBonus].
     *
     * Delays only apply to distracting apps, *unless* friction is HARDCORE — at which point
     * the launcher slows down everything. Returns 0 when the app is exempt.
     */
    fun delaySeconds(
        baseDelaySeconds: Int,
        level: FrictionLevel,
        slowMode: Boolean,
        riskTier: RiskTier,
        isDistracting: Boolean,
    ): Int {
        val appliesToThisApp = isDistracting || level == FrictionLevel.HARDCORE
        if (!appliesToThisApp) return 0
        val base = baseDelaySeconds.coerceAtLeast(0)
        val scaled = base * delayMultiplier(level)
        val slowBonus = if (slowMode) 2 else 0
        return (scaled + slowBonus + riskDelayBonus(riskTier)).coerceAtLeast(0)
    }
}
