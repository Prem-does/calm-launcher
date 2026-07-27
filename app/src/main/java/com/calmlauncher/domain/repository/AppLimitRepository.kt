package com.calmlauncher.domain.repository

import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitGroupAssignment
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.AppLimitUsage
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
    suspend fun extendOverride(packageName: String, minutes: Int): Boolean
    suspend fun scheduleApproachAlarms(packageName: String)
    suspend fun refreshUsageSnapshot()
    suspend fun recordBlockedLaunch(status: AppLimitStatus)
    suspend fun todaySummary(): AppLimitSummary
    suspend fun evaluate(packageName: String, label: String): AppLimitDecision
}
