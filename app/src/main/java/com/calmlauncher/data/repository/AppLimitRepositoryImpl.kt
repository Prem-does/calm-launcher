package com.calmlauncher.data.repository

import com.calmlauncher.data.db.AppLimitDao
import com.calmlauncher.data.db.toDomain
import com.calmlauncher.data.db.toEntity
import com.calmlauncher.data.system.ClockTicker
import com.calmlauncher.data.system.UsageStatsTracker
import com.calmlauncher.di.IoDispatcher
import com.calmlauncher.notification.LimitNotificationManager
import com.calmlauncher.domain.repository.AppRepository
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.calmlauncher.notification.LimitAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitEventType
import com.calmlauncher.domain.model.AppLimitGroupAssignment
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.AppLimitUsage
import com.calmlauncher.domain.repository.AppLimitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val appRepository: AppRepository,
    private val notifications: LimitNotificationManager,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : AppLimitRepository {

    private companion object {
        private const val MAX_DAILY_OVERRIDES = 2

        /** Start warning once this little of the daily allowance is left. */
        private const val WARNING_WINDOW_MINUTES = 10

        /** Never re-notify about the same app more often than this. */
        private const val NOTIFY_SUPPRESSION_MS = 5L * 60_000L
    }

    override fun hasUsageAccess(): Boolean = usageStatsTracker.hasPermission()

    override fun observeRules(): Flow<List<AppLimitRule>> =
        appLimitDao.observeRules().map { rows -> rows.map { it.toDomain() } }.flowOn(dispatcher)

    override fun observeGroupAssignments(): Flow<List<AppLimitGroupAssignment>> =
        appLimitDao.observeGroupAssignments().map { rows -> rows.map { it.toDomain() } }.flowOn(dispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTodayUsage(): Flow<List<AppLimitUsage>> =
        clockTicker.time
            .map { todayDayStart(it) }
            .distinctUntilChanged()
            .flatMapLatest { dayStart ->
                appLimitDao.observeUsage(dayStart).map { rows -> rows.map { it.toDomain() } }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
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

    override suspend fun saveGroupAssignments(groupId: String, packageNames: Set<String>) = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        val sortedPackages = packageNames.sorted()
        appLimitDao.deleteGroupAssignments(groupId)
        if (sortedPackages.isNotEmpty()) {
            appLimitDao.deleteAssignmentsForPackages(sortedPackages)
            appLimitDao.upsertGroupAssignments(
                sortedPackages.map { packageName ->
                    AppLimitGroupAssignment(
                        groupId = groupId,
                        packageName = packageName,
                        updatedAtEpochMs = now,
                    ).toEntity()
                },
            )
        }
    }

    override suspend fun packagesInGroup(groupId: String): Set<String> = withContext(dispatcher) {
        appLimitDao.getPackagesInGroup(groupId).toSet()
    }

    override suspend fun deleteRule(packageName: String) = withContext(dispatcher) {
        appLimitDao.deleteRule(packageName)
        // The rule is gone; any warning still in the shade is now a lie.
        notifications.clear(packageName)
    }

    override suspend fun setEnabled(packageName: String, enabled: Boolean) = withContext(dispatcher) {
        val current = appLimitDao.getRule(packageName)?.toDomain() ?: AppLimitRule(packageName = packageName)
        appLimitDao.upsertRule(current.copy(enabled = enabled, updatedAtEpochMs = System.currentTimeMillis()).toEntity())
        if (!enabled) notifications.clear(packageName)
    }

    override suspend fun extendOverride(packageName: String, minutes: Int): Boolean = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        val dayStart = todayDayStart(now)
        val current = appLimitDao.getRule(packageName)?.toDomain() ?: AppLimitRule(packageName = packageName)
        val overridesUsedToday = todayOverrideCount(packageName, dayStart)
        if (overridesUsedToday >= MAX_DAILY_OVERRIDES) return@withContext false

        // Accumulate remaining override time when the user is already inside an allowed window.
        val existingRemainingMs = (current.overrideUntilEpochMs - now).coerceAtLeast(0L)
        val addedMs = minutes * 60_000L
        val newRemainingMs = existingRemainingMs + addedMs
        val newOverrideUntil = now + newRemainingMs

        appLimitDao.upsertRule(
            current.copy(
                overrideUntilEpochMs = newOverrideUntil,
                updatedAtEpochMs = now,
            ).toEntity(),
        )
        // The user bought themselves time — clear the "you're done" notification.
        notifications.clear(packageName)

        appLimitDao.upsertEvent(
            AppLimitEvent(
                packageName = packageName,
                label = runCatching { appRepository.getApp(packageName)?.label ?: packageName }.getOrNull() ?: packageName,
                eventType = AppLimitEventType.OVERRIDE,
                timestampEpochMs = now,
                dayStartEpochMs = dayStart,
                limitMinutes = current.dailyLimitMinutes,
                usedMinutes = (usageStatsTracker.todayForegroundFor(packageName) / 60_000L).toInt(),
                overrideMinutes = minutes,
            ).toEntity(),
        )
        true
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
        syncLimitNotifications(snapshot.perApp)
    }

    /**
     * Bring the notification shade in line with current usage.
     *
     * The old implementation only fired on an *exact* remaining value of 10/5/1 minutes, which
     * a 15-minute rollup almost always steps straight over. Instead we notify whenever the app
     * is inside the warning window and re-state the real remaining time, rate-limited by
     * [NOTIFY_SUPPRESSION_MS]. Notifications are also actively cleared once a rule stops
     * applying, so a stale "5 minutes left" can't linger after the user disables the limit.
     */
    private suspend fun syncLimitNotifications(usageByPackage: Map<String, Long>) {
        runCatching {
            val nowMs = System.currentTimeMillis()
            appLimitDao.getAllRules().map { it.toDomain() }.forEach { rule ->
                val usedMs = usageByPackage[rule.packageName] ?: 0L
                val limitMs = rule.dailyLimitMinutes * 60_000L
                val remainingMinutes = ((limitMs - usedMs) / 60_000L).toInt()

                // Rule off, overridden, or comfortably under the limit — nothing to say.
                if (!rule.enabled ||
                    rule.overrideUntilEpochMs > nowMs ||
                    remainingMinutes > WARNING_WINDOW_MINUTES
                ) {
                    notifications.clear(rule.packageName)
                    return@forEach
                }

                if (nowMs - rule.lastNotifiedEpochMs < NOTIFY_SUPPRESSION_MS) return@forEach

                val label = runCatching { appRepository.getApp(rule.packageName)?.label }
                    .getOrNull() ?: rule.packageName
                if (remainingMinutes <= 0) {
                    notifications.notifyLimitReached(rule.packageName, label)
                } else {
                    notifications.notifyApproachingLimit(rule.packageName, label, remainingMinutes)
                }
                appLimitDao.upsertRule(rule.copy(lastNotifiedEpochMs = nowMs).toEntity())
            }
        }
        // Best-effort only; a notification failure must never break the usage rollup.
    }

    override suspend fun scheduleApproachAlarms(packageName: String) = withContext(dispatcher) {
        val rule = appLimitDao.getRule(packageName)?.toDomain() ?: return@withContext
        if (!rule.enabled) return@withContext
        val now = System.currentTimeMillis()
        if (rule.overrideUntilEpochMs > now) return@withContext
        val usedMs = usageStatsTracker.todayForegroundFor(packageName)
        val limitMs = rule.dailyLimitMinutes * 60_000L
        val remainingMs = (limitMs - usedMs).coerceAtLeast(0L)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@withContext

        // 0 is the "limit reached" alarm; the rest are the countdown warnings.
        listOf(10, 5, 1, 0).forEach { minutes ->
            val whenMs = now + remainingMs - minutes * 60_000L
            if (whenMs <= now) return@forEach
            val intent = Intent(context, LimitAlarmReceiver::class.java).apply {
                putExtra(LimitAlarmReceiver.EXTRA_PACKAGE, packageName)
                putExtra(LimitAlarmReceiver.EXTRA_REMAINING_MINUTES, minutes)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                "${packageName}_$minutes".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            runCatching {
                // Exact alarms need a user grant on Android 12+; degrade to an inexact one
                // rather than throwing, since a slightly late warning still beats none.
                if (canScheduleExact(am)) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
                }
            }
        }
    }

    private fun canScheduleExact(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

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
            overridesUsedToday = todayOverrideCount(packageName, todayDayStart(now)),
            overrideLimitPerDay = MAX_DAILY_OVERRIDES,
        )
        recordBlockedLaunch(status)
        refreshUsageSnapshot()
        AppLimitDecision.Blocked(status)
    }

    private suspend fun todayOverrideCount(packageName: String, dayStartEpochMs: Long): Int =
        appLimitDao.observeEvents(dayStartEpochMs)
            .first()
            .count { row ->
                row.packageName == packageName && row.eventType == AppLimitEventType.OVERRIDE.name
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
