package com.calmlauncher.domain.policy

import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.EnvironmentMode

/**
 * Pure mapping from an [EnvironmentMode] to the set of [AppCategory] values it hard-blocks.
 *
 * Environment modes reshape blocking independently of, and in addition to, an explicit
 * focus session:
 *
 *  - SLEEP blocks SOCIAL, ENTERTAINMENT, BROWSER, GAME.
 *  - WORK and STUDY block SOCIAL, ENTERTAINMENT, GAME.
 *  - DEEP_WORK blocks SOCIAL, ENTERTAINMENT, BROWSER, STORE, GAME.
 *  - GYM, OUTSIDE, and TRAVEL keep tools relaxed but block SOCIAL and GAME during focus.
 *  - NONE blocks nothing.
 *
 * All functions are pure: no I/O, no time, no Android.
 */
internal object EnvironmentRules {

    /**
     * Categories blocked purely by the [mode] itself, independent of any focus session.
     * GYM/OUTSIDE/TRAVEL return an empty set here because their feed block is conditional
     * on an active focus session; see [blocks].
     */
    private fun unconditionalBlocks(mode: EnvironmentMode): Set<AppCategory> = when (mode) {
        EnvironmentMode.NONE -> emptySet()
        EnvironmentMode.SLEEP -> setOf(
            AppCategory.SOCIAL,
            AppCategory.ENTERTAINMENT,
            AppCategory.BROWSER,
            AppCategory.GAME,
        )
        EnvironmentMode.WORK, EnvironmentMode.STUDY -> setOf(
            AppCategory.SOCIAL,
            AppCategory.ENTERTAINMENT,
            AppCategory.GAME,
        )
        EnvironmentMode.DEEP_WORK -> setOf(
            AppCategory.SOCIAL,
            AppCategory.ENTERTAINMENT,
            AppCategory.BROWSER,
            AppCategory.STORE,
            AppCategory.GAME,
        )
        EnvironmentMode.GYM, EnvironmentMode.OUTSIDE, EnvironmentMode.TRAVEL -> emptySet()
    }

    /**
     * True if [mode] blocks [category]. [focusActive] only matters for GYM, OUTSIDE,
     * and TRAVEL, where SOCIAL and GAME are blocked while a focus session is running.
     */
    fun blocks(mode: EnvironmentMode, category: AppCategory, focusActive: Boolean): Boolean {
        if (category in unconditionalBlocks(mode)) return true
        val relaxesToolsButBlocksFeeds = mode == EnvironmentMode.GYM ||
            mode == EnvironmentMode.OUTSIDE ||
            mode == EnvironmentMode.TRAVEL
        return relaxesToolsButBlocksFeeds && focusActive &&
            (category == AppCategory.SOCIAL || category == AppCategory.GAME)
    }

    /** A short, mode-specific reason string for a [com.calmlauncher.domain.model.FrictionStep.Block]. */
    fun blockReason(mode: EnvironmentMode): String = when (mode) {
        EnvironmentMode.SLEEP -> "Blocked in Sleep mode"
        EnvironmentMode.WORK -> "Blocked in Work mode"
        EnvironmentMode.STUDY -> "Blocked in Study mode"
        EnvironmentMode.DEEP_WORK -> "Blocked in Deep Work mode"
        EnvironmentMode.GYM -> "Blocked in Gym mode"
        EnvironmentMode.OUTSIDE -> "Blocked in Outside mode"
        EnvironmentMode.TRAVEL -> "Blocked in Travel mode"
        EnvironmentMode.NONE -> "Blocked"
    }
}
