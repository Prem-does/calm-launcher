package com.calmlauncher.data.repository

import com.calmlauncher.data.db.AppLimitDao
import com.calmlauncher.data.db.toDomain
import com.calmlauncher.data.db.toEntity
import com.calmlauncher.data.system.ClockTicker
import com.calmlauncher.data.system.UsageStatsTracker
import com.calmlauncher.di.IoDispatcher
import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitEventType
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.AppLimitUsage
import com.calmlauncher.domain.repository.AppLimitRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppLimitRepositoryImpl @Inject constructor(
    private val appLimitDao: AppLimitDao,
    private val clockTicker: ClockTicker,
    private val usageStatsTracker: UsageStatsTracker,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : AppLimitRepository {

    override fun observeRules(): Flow<List<AppLimitRule>> =
        appLimitDao.observeRules().map { rows -> rows.map { it.toDomain() } }.flowOn(dispatcher)

    override fun observeTodayUsage(): Flow<List<AppLimitUsage>> =
        clockTicker.time
            .map { todayDayStart(it) }
            .distinctUntilChanged()
            .flatMapLatest { dayStart ->
                appLimitDao.observeUsage(dayStart).map { rows -> rows.map { it.toDomain() } }
            }

    override fun observeTodayEvents(): Flow<List<AppLimitEvent>> =
        clockTicker.time
            .map { todayDayStart(it) }
            .distinctUntilChanged()
            .flatMapLatest { dayStart ->
                appLimitDao.observeEvents(dayStart).map { rows -> rows.map { it.toDomain() } }
            }

    override suspend fun currentRule(packageName: String): AppLimitRule? = withContext(dispatcher) {
        appLimitDao.getRule(packageName)?.toDomain()
    }

    override suspend fun saveRule(rule: AppLimitRule) = withContext(dispatcher) {
        appLimitDao.upsertRule(rule.toEntity())
    }

    override suspend fun deleteRule(packageName: String) = withContext(dispatcher) {
        appLimitDao.deleteRule(packageName)
    }

    override suspend fun setEnabled(packageName: String, enabled: Boolean) = withContext(dispatcher) {
        val current = appLimitDao.getRule(packageName)?.toDomain() ?: AppLimitRule(packageName = packageName)
        appLimitDao.upsertRule(current.copy(enabled = enabled, updatedAtEpochMs = System.currentTimeMillis()).toEntity())
    }

    override suspend fun extendOverride(packageName: String, minutes: Int) = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        val current = appLimitDao.getRule(packageName)?.toDomain() ?: AppLimitRule(packageName = packageName)
        appLimitDao.upsertRule(
            current.copy(
                overrideUntilEpochMs = now + minutes * 60_000L,
                updatedAtEpochMs = now,
            ).toEntity(),
        )
    }

    override suspend fun refreshUsageSnapshot() = withContext(dispatcher) {
        val snapshot = usageStatsTracker.todayForeground()
        val now = System.currentTimeMillis()
        snapshot.perApp.forEach { (packageName, usedMs) ->
            appLimitDao.upsertUsage(
                AppLimitUsage(
                    dayStartEpochMs = snapshot.dayStartEpochMs,
                    packageName = packageName,
                    usedMs = usedMs,
                    lastSyncedAtEpochMs = now,
                ).toEntity(),
            )
        }
        appLimitDao.deleteUsageBefore(snapshot.dayStartEpochMs)
    }

    override suspend fun recordBlockedLaunch(status: AppLimitStatus) = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        appLimitDao.upsertEvent(
            AppLimitEvent(
                packageName = status.packageName,
                label = status.label,
                eventType = AppLimitEventType.BLOCKED,
                timestampEpochMs = now,
                dayStartEpochMs = todayDayStart(now),
                limitMinutes = status.dailyLimitMinutes ?: 0,
                usedMinutes = status.usedMinutes,
                overrideMinutes = 0,
            ).toEntity(),
        )
    }

    override suspend fun todaySummary(): AppLimitSummary = withContext(dispatcher) {
        val events = appLimitDao.observeEvents(todayDayStart()).first().map { it.toDomain() }
        val blocked = events.filter { it.eventType == AppLimitEventType.BLOCKED }
        val counts = blocked.groupingBy { it.packageName }.eachCount()
        val top = counts.maxByOrNull { it.value }
        AppLimitSummary(
            blockedLaunchesToday = blocked.size,
            limitedAppsToday = counts.size,
            estimatedTimeSavedMinutes = blocked.sumOf { 10L }.toInt(),
            topLimitedPackage = top?.key,
            topLimitedCount = top?.value ?: 0,
        )
    }

    override suspend fun evaluate(packageName: String, label: String): AppLimitDecision = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        val rule = appLimitDao.getRule(packageName)?.toDomain() ?: return@withContext AppLimitDecision.Allowed
        if (!rule.enabled) return@withContext AppLimitDecision.Allowed
        if (rule.overrideUntilEpochMs > now) return@withContext AppLimitDecision.Allowed

        val usedMs = usageStatsTracker.todayForegroundFor(packageName)
        val limitMs = rule.dailyLimitMinutes * 60_000L
        if (usedMs < limitMs) {
            refreshUsageSnapshot()
            return@withContext AppLimitDecision.Allowed
        }

        val status = AppLimitStatus(
            packageName = packageName,
            label = label,
            enabled = true,
            dailyLimitMinutes = rule.dailyLimitMinutes,
            usedMinutes = (usedMs / 60_000L).toInt(),
            overrideUntilEpochMs = rule.overrideUntilEpochMs,
            blockedToday = true,
        )
        recordBlockedLaunch(status)
        refreshUsageSnapshot()
        AppLimitDecision.Blocked(status)
    }

    private fun todayDayStart(nowEpochMs: Long = System.currentTimeMillis()): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = nowEpochMs
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
