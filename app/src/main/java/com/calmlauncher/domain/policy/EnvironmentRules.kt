package com.calmlauncher.domain.policy

import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.EnvironmentMode

/**
 * Pure mapping from an [EnvironmentMode] to the set of [AppCategory] values it hard-blocks.
 *
 * Environment modes re-shape blocking independently of (and in addition to) an explicit
 * focus session. The table below is the single source of truth referenced by the spec:
 *
 *  - SLEEP            blocks SOCIAL, ENTERTAINMENT, BROWSER, GAME (wind-down at night).
 *  - STUDY, DEEP_WORK block SOCIAL, ENTERTAINMENT, GAME (browsers/tools relaxed so you
 *                     can still research, but the obvious dopamine sinks are gone).
 *  - GYM, OUTSIDE     relax tools entirely and only block SOCIAL — and then *only when a
 *                     focus session is active* (you're out living life; the phone should
 *                     stay a tool, but we don't fully lock it down unless you opted in).
 *  - NONE             blocks nothing.
 *
 * All functions are pure (no I/O, no time, no Android).
 */
internal object EnvironmentRules {

    /**
     * Categories blocked purely by the [mode] itself, independent of any focus session.
     * GYM/OUTSIDE return an empty set here because their (social) block is conditional on
     * an active focus session — see [blocks].
     */
    private fun unconditionalBlocks(mode: EnvironmentMode): Set<AppCategory> = when (mode) {
        EnvironmentMode.NONE -> emptySet()
        EnvironmentMode.SLEEP -> setOf(
            AppCategory.SOCIAL,
            AppCategory.ENTERTAINMENT,
            AppCategory.BROWSER,
            AppCategory.GAME,
        )
        EnvironmentMode.STUDY, EnvironmentMode.DEEP_WORK -> setOf(
            AppCategory.SOCIAL,
            AppCategory.ENTERTAINMENT,
            AppCategory.GAME,
        )
        // Tools are relaxed when out in the world; the social block is conditional.
        EnvironmentMode.GYM, EnvironmentMode.OUTSIDE -> emptySet()
    }

    /**
     * True if [mode] blocks [category]. [focusActive] only matters for GYM/OUTSIDE, where
     * SOCIAL is blocked while a focus session is running but otherwise left reachable.
     */
    fun blocks(mode: EnvironmentMode, category: AppCategory, focusActive: Boolean): Boolean {
        if (category in unconditionalBlocks(mode)) return true
        val relaxesToolsButBlocksSocial =
            mode == EnvironmentMode.GYM || mode == EnvironmentMode.OUTSIDE
        return relaxesToolsButBlocksSocial && focusActive && category == AppCategory.SOCIAL
    }

    /** A short, mode-specific reason string for a [com.calmlauncher.domain.model.FrictionStep.Block]. */
    fun blockReason(mode: EnvironmentMode): String = when (mode) {
        EnvironmentMode.SLEEP -> "Blocked in Sleep mode"
        EnvironmentMode.STUDY -> "Blocked in Study mode"
        EnvironmentMode.DEEP_WORK -> "Blocked in Deep Work mode"
        EnvironmentMode.GYM -> "Blocked in Gym mode"
        EnvironmentMode.OUTSIDE -> "Blocked in Outside mode"
        EnvironmentMode.NONE -> "Blocked"
    }
}
