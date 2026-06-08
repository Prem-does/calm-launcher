package com.calmlauncher.domain.repository

import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.AppLimitUsage
import kotlinx.coroutines.flow.Flow

interface AppLimitRepository {
    fun observeRules(): Flow<List<AppLimitRule>>
    fun observeTodayUsage(): Flow<List<AppLimitUsage>>
    fun observeTodayEvents(): Flow<List<AppLimitEvent>>
    suspend fun currentRule(packageName: String): AppLimitRule?
    suspend fun saveRule(rule: AppLimitRule)
    suspend fun deleteRule(packageName: String)
    suspend fun setEnabled(packageName: String, enabled: Boolean)
    suspend fun extendOverride(packageName: String, minutes: Int): Boolean
    suspend fun scheduleApproachAlarms(packageName: String)
    suspend fun refreshUsageSnapshot()
    suspend fun recordBlockedLaunch(status: AppLimitStatus)
    suspend fun todaySummary(): AppLimitSummary
    suspend fun evaluate(packageName: String, label: String): AppLimitDecision
}
