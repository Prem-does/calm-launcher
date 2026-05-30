package com.calmlauncher.domain.model

/**
 * Escalating usage-risk tiers. Higher tiers tighten friction and minimalism and can
 * trigger Recovery Mode. Low risk relaxes the interface (Reward Real Life).
 */
enum class RiskTier { CALM, ELEVATED, HIGH, RECOVERY }

/**
 * Output of the Dopamine Detection Engine — a rolling assessment of how compulsive
 * recent usage looks. Persisted so it survives process death.
 */
data class RiskState(
    val tier: RiskTier = RiskTier.CALM,
    val score: Int = 0, // 0..100
    val repeatedOpens: Int = 0,
    val rapidSwitches: Int = 0,
    val lateNightLaunches: Int = 0,
    val longestSessionMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
)

/**
 * Derived, observable UI posture. Screens collect this and adapt: enforce grayscale,
 * hide suggestions/recents, raise minimalism, and disable motion. Produced by the
 * ModeEngine from settings + [RiskState] + environment.
 */
data class UiRestrictionState(
    val grayscale: Boolean = false,
    val grayscaleAmount: Float = 0f,
    val hideSuggestions: Boolean = true,
    val hideRecents: Boolean = true,
    val minimalismLevel: Int = 0, // 0..3
    val motionEnabled: Boolean = true,
    val forceFocusSuggested: Boolean = false,
    val riskTier: RiskTier = RiskTier.CALM,
)
