package com.calmlauncher.domain.repository

import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitExtensionCaps
import com.calmlauncher.domain.model.AppLimitGroupAssignment
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.AppLimitUsage
import com.calmlauncher.domain.model.OverrideResult
import kotlinx.coroutines.flow.Flow

interface AppLimitRepository {
    /**
     * Whether Usage Access has been granted. Without it every usage read returns zero, so
     * limits can never trigger — the UI has to say so rather than showing a silent "0m used".
     */
    fun hasUsageAccess(): Boolean

    fun observeRules(): Flow<List<AppLimitRule>>
    fun observeGroupAssignments(): Flow<List<AppLimitGroupAssignment>>
    fun observeTodayUsage(): Flow<List<AppLimitUsage>>
    fun observeTodayEvents(): Flow<List<AppLimitEvent>>
    suspend fun currentRule(packageName: String): AppLimitRule?
    suspend fun saveRule(rule: AppLimitRule)
    suspend fun saveGroupAssignments(groupId: String, packageNames: Set<String>)

    /** Packages currently assigned to [groupId]. Used to spot apps dropped from a group. */
    suspend fun packagesInGroup(groupId: String): Set<String>

    suspend fun deleteRule(packageName: String)
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    /**
     * Ask for [minutes] more time on [packageName].
     *
     * Returns a typed [OverrideResult] rather than a boolean, and callers **must** honour a
     * [OverrideResult.Denied] by keeping the app blocked. This is the contract that closes the
     * original exploit: the old signature returned `false` on refusal and the block overlay
     * dismissed itself regardless of the answer, so pressing "Add 10 minutes" bought free time
     * whether or not any was actually granted.
     *
     * The minutes actually granted may be fewer than requested when the day's budget is nearly
     * spent — see [OverrideResult.Granted.grantedMinutes] — and are always clamped to
     * [com.calmlauncher.domain.model.AppLimitCeilings.MAX_MINUTES_PER_EXTENSION], so no caller can
     * ask for an arbitrary amount of time.
     */
    suspend fun extendOverride(packageName: String, minutes: Int): OverrideResult

    /** Current usage and extension budget for one app, or null when it has no limit rule. */
    suspend fun statusFor(packageName: String, label: String): AppLimitStatus?

    /** How many minutes a single extension is worth, per user configuration. */
    suspend fun minutesPerExtension(): Int

    /**
     * Change the daily extension budget. Tightening applies immediately; relaxing takes effect at
     * the next daily reset so the setting can't be used to unblock an app that is blocked right
     * now. Returns true when the change is already in force, false when it is pending.
     */
    suspend fun setExtensionCaps(extensionsPerDay: Int, extraMinutesPerDay: Int): Boolean

    /** The extension budget currently in force. */
    suspend fun currentExtensionCaps(): AppLimitExtensionCaps

    suspend fun scheduleApproachAlarms(packageName: String)

    /**
     * Bring [packageName]'s limit notification in line with its current usage, posting only if
     * that says something the user has not already been told today. Safe to call repeatedly —
     * this is the one path both the threshold alarm and the usage rollup share, and it is what
     * keeps a single app from collecting four copies of the same warning.
     */
    suspend fun syncLimitNotification(packageName: String)

    suspend fun refreshUsageSnapshot()
    suspend fun recordBlockedLaunch(status: AppLimitStatus)
    suspend fun todaySummary(): AppLimitSummary
    suspend fun evaluate(packageName: String, label: String): AppLimitDecision
}
