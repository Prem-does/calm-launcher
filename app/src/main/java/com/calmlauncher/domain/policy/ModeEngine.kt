package com.calmlauncher.domain.policy

import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.LaunchDecision
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.model.RiskState
import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.model.UiRestrictionState

/**
 * The heart of the launcher. Every app open flows through [resolve], which composes
 * all enabled modes (friction tiers, slow/analog, breath, intent prompt, focus rules,
 * dead-end feeds, one-app-at-a-time, environment & recovery) into an ordered
 * [LaunchDecision]. [restrictionState] derives the observable UI posture (grayscale,
 * minimalism, motion, hidden surfaces) for every screen.
 *
 * Implementations must be pure functions of their inputs (no I/O) so they are trivially
 * testable and free of timing surprises.
 */
interface ModeEngine {

    fun resolve(
        request: AppLaunchRequest,
        settings: LauncherSettings,
        risk: RiskState,
        recentEvents: List<LaunchEvent>,
        nowEpochMs: Long,
    ): LaunchDecision

    fun restrictionState(
        settings: LauncherSettings,
        risk: RiskState,
    ): UiRestrictionState

    /** True if a focus session / environment rule should hard-block this app. */
    fun isFocusBlocked(
        packageName: String,
        category: AppCategory,
        settings: LauncherSettings,
    ): Boolean
}
