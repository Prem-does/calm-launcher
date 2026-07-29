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
import com.calmlauncher.domain.model.LimitNotifyStage
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
        if (!enabled) {
            notifications.clear(packageName)
            resetNotificationStage(packageName)
        }
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
        // The user bought themselves time — clear the "you're done" notification, and forget
        // the stage so the countdown can speak again when the extension runs out.
        notifications.clear(packageName)
        resetNotificationStage(packageName)

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

    override suspend fun syncLimitNotification(packageName: String) = withContext(dispatcher) {
        val rule = appLimitDao.getRule(packageName)?.toDomain() ?: return@withContext
        val usedMs = runCatching { usageStatsTracker.todayForegroundFor(packageName) }.getOrDefault(0L)
        syncNotificationFor(rule, usedMs, System.currentTimeMillis())
    }

    /** Re-evaluate every rule against a fresh usage snapshot. */
    private suspend fun syncLimitNotifications(usageByPackage: Map<String, Long>) {
        // Best-effort only; a notification failure must never break the usage rollup.
        runCatching {
            val nowMs = System.currentTimeMillis()
            appLimitDao.getAllRules().map { it.toDomain() }.forEach { rule ->
                syncNotificationFor(rule, usageByPackage[rule.packageName] ?: 0L, nowMs)
            }
        }
    }

    /**
     * The single place that decides whether an app-limit notification gets posted.
     *
     * Both the exact threshold alarm and the 15-minute usage rollup land here, and neither can
     * tell the other what it has already said — so the *rule itself* remembers, as a
     * [LimitNotifyStage] stamped with the day it belongs to. A sync posts only when it can move
     * that stage forward, which makes repeat calls silent no matter how many fire for the same
     * moment. Two consequences worth stating plainly:
     *
     *  - "Limit reached" is told exactly once per app per day, not once per rollup.
     *  - A 15-minute rollup that steps straight over the 5-minute mark still warns, because the
     *    stage is derived from *current* remaining time rather than an exact tripwire value.
     *
     * The stage also walks backwards, but only for the right reasons: a new day, a granted
     * override, a disabled rule, or a raised limit all reset it to [LimitNotifyStage.NONE] and
     * clear the shade, so a stale "5 minutes left" can't outlive the thing it described.
     */
    private suspend fun syncNotificationFor(rule: AppLimitRule, usedMs: Long, nowMs: Long) {
        val dayStart = todayDayStart(nowMs)
        // A stage recorded yesterday says nothing about today's allowance.
        val recordedStage = if (rule.lastNotifiedDayStartEpochMs == dayStart) {
            rule.lastNotifiedStage
        } else {
            LimitNotifyStage.NONE
        }

        val limitMs = rule.dailyLimitMinutes * 60_000L
        val remainingMinutes = ((limitMs - usedMs) / 60_000L).toInt()
        val suppressed = !rule.enabled || rule.overrideUntilEpochMs > nowMs
        val targetStage = if (suppressed) {
            LimitNotifyStage.NONE
        } else {
            LimitNotifyStage.forRemaining(remainingMinutes)
        }

        if (targetStage == recordedStage) {
            // Nothing has changed since the last sync. Still re-stamp the day so a rule that
            // sat at NONE overnight doesn't look like it belongs to an older day.
            if (rule.lastNotifiedDayStartEpochMs != dayStart) {
                persistStage(rule, LimitNotifyStage.NONE, dayStart, rule.lastNotifiedEpochMs)
            }
            return
        }

        if (targetStage == LimitNotifyStage.NONE) {
            // Back under the warning window, overridden, or switched off.
            notifications.clear(rule.packageName)
            persistStage(rule, LimitNotifyStage.NONE, dayStart, rule.lastNotifiedEpochMs)
            return
        }

        // Never re-announce a threshold already passed today — usage only goes up, so a lower
        // stage here means a slightly stale reading, not new information.
        if (targetStage.ordinal < recordedStage.ordinal) return

        val label = runCatching { appRepository.getApp(rule.packageName)?.label }
            .getOrNull() ?: rule.packageName
        notifications.notifyStage(rule.packageName, label, targetStage, remainingMinutes)
        persistStage(rule, targetStage, dayStart, nowMs)
    }

    private suspend fun persistStage(
        rule: AppLimitRule,
        stage: LimitNotifyStage,
        dayStart: Long,
        notifiedAtMs: Long,
    ) {
        appLimitDao.upsertRule(
            rule.copy(
                lastNotifiedEpochMs = notifiedAtMs,
                lastNotifiedStage = stage,
                lastNotifiedDayStartEpochMs = dayStart,
            ).toEntity(),
        )
    }

    /** Forget what we've told the user about [packageName], so the next real change speaks. */
    private suspend fun resetNotificationStage(packageName: String) {
        val rule = appLimitDao.getRule(packageName)?.toDomain() ?: return
        persistStage(rule, LimitNotifyStage.NONE, todayDayStart(), rule.lastNotifiedEpochMs)
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
