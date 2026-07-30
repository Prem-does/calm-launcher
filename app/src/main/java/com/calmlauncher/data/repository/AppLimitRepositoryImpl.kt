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
import com.calmlauncher.domain.model.AppLimitCeilings
import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitEventType
import com.calmlauncher.domain.model.AppLimitExtensionCaps
import com.calmlauncher.domain.model.AppLimitGroupAssignment
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.AppLimitUsage
import com.calmlauncher.domain.model.LimitNotifyStage
import com.calmlauncher.domain.model.OverrideDenialReason
import com.calmlauncher.domain.model.OverrideResult
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository,
    private val notifications: LimitNotificationManager,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : AppLimitRepository {

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

    /**
     * Write a rule, preserving the bookkeeping the caller has no business setting.
     *
     * This is a deliberate choke point. Callers build an [AppLimitRule] to express intent — which
     * app, enabled or not, how many minutes — and construct it fresh, leaving the extension ledger
     * and the active override window at their defaults. Upserting that directly is how the old code
     * let a user reset their own extension budget by saving a group limit, or clear an in-flight
     * override by toggling a preset. So the durable fields are taken from the stored row and the
     * caller's values for them are ignored, rather than trusting every call site to remember.
     */
    override suspend fun saveRule(rule: AppLimitRule) = withContext(dispatcher) {
        val existing = appLimitDao.getRule(rule.packageName)?.toDomain()
        val merged = if (existing == null) {
            rule
        } else {
            rule.copy(
                overrideUntilEpochMs = existing.overrideUntilEpochMs,
                overrideDayStartEpochMs = existing.overrideDayStartEpochMs,
                overridesUsedToday = existing.overridesUsedToday,
                overrideMinutesUsedToday = existing.overrideMinutesUsedToday,
                lastNotifiedEpochMs = existing.lastNotifiedEpochMs,
                // A changed limit invalidates what the user was last told about their remaining
                // time, so the countdown is allowed to speak again — but only that.
                lastNotifiedStage = if (existing.dailyLimitMinutes == rule.dailyLimitMinutes) {
                    existing.lastNotifiedStage
                } else {
                    LimitNotifyStage.NONE
                },
                lastNotifiedDayStartEpochMs = existing.lastNotifiedDayStartEpochMs,
            )
        }
        appLimitDao.upsertRule(merged.toEntity())
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

    /**
     * The extension budget in force right now, promoting any pending raise whose day has come.
     *
     * A *lowered* cap is written straight to the effective fields by the settings layer, because
     * tightening your own limits must never be deferred. A *raised* cap is parked as pending and
     * only promoted here, on a later day than it was requested. That single asymmetry is what stops
     * the settings screen from becoming a second unlimited-extension button: while an app is
     * blocked, raising the cap changes nothing until tomorrow.
     */
    private suspend fun activeCaps(nowMs: Long): AppLimitExtensionCaps {
        val settings = settingsRepository.current()
        val dayStart = todayDayStart(nowMs)
        val pendingExtensions = settings.pendingLimitExtensionsPerDay
        val pendingMinutes = settings.pendingLimitExtraMinutesPerDay
        val hasPending = pendingExtensions >= 0 || pendingMinutes >= 0
        val pendingIsDue = hasPending && dayStart > settings.limitCapsPendingSinceDayStartEpochMs

        if (!pendingIsDue) {
            return AppLimitExtensionCaps(
                extensionsPerDay = settings.limitExtensionsPerDay,
                extraMinutesPerDay = settings.limitExtraMinutesPerDay,
            ).clamped()
        }

        val promoted = AppLimitExtensionCaps(
            extensionsPerDay = if (pendingExtensions >= 0) {
                pendingExtensions
            } else {
                settings.limitExtensionsPerDay
            },
            extraMinutesPerDay = if (pendingMinutes >= 0) {
                pendingMinutes
            } else {
                settings.limitExtraMinutesPerDay
            },
        ).clamped()

        settingsRepository.update {
            it.copy(
                limitExtensionsPerDay = promoted.extensionsPerDay,
                limitExtraMinutesPerDay = promoted.extraMinutesPerDay,
                pendingLimitExtensionsPerDay = -1,
                pendingLimitExtraMinutesPerDay = -1,
                limitCapsPendingSinceDayStartEpochMs = 0L,
            )
        }
        return promoted
    }

    /**
     * Grant more time for [packageName], or explain why not.
     *
     * This method is the fix for the "press Add 10 Minutes forever" exploit, and it is worth being
     * explicit about why the old version failed. It counted OVERRIDE rows in the event log, decided
     * in Kotlin whether there was room, and then wrote the grant. Three separate weaknesses fell
     * out of that:
     *
     *  1. **Nothing bounded the total time.** Only the *count* of extensions was checked, so a
     *     caller passing a large `minutes` — or accumulating a leftover window, as it did — could
     *     turn two extensions into hours.
     *  2. **The ledger was rebuildable.** Saving a group limit reconstructed the rule from
     *     scratch, resetting `overrideUntilEpochMs`; anything that rewrote the rule handed back a
     *     fresh allowance.
     *  3. **Check and write were not atomic.** Two quick taps both read "one left" and both spent
     *     it.
     *
     * Now: the budget lives on the rule, both a count *and* a minute total are enforced, the spend
     * happens inside a single conditional UPDATE ([AppLimitDao.spendOverrideBudget]), and the event
     * log is consulted as a floor so deleting and re-adding a rule can't wipe the day's spending.
     * A refusal is returned as a typed [OverrideResult] rather than a bare `false`, because the
     * caller has to be able to tell the user *why* — and, critically, must not dismiss the block
     * screen when the answer is no.
     */
    override suspend fun extendOverride(packageName: String, minutes: Int): OverrideResult =
        withContext(dispatcher) {
            val now = System.currentTimeMillis()
            val dayStart = todayDayStart(now)
            val rule = appLimitDao.getRule(packageName)?.toDomain()
                ?: return@withContext OverrideResult.Denied(OverrideDenialReason.NO_RULE)
            if (!rule.enabled) {
                return@withContext OverrideResult.Denied(OverrideDenialReason.NO_RULE)
            }

            val caps = activeCaps(now)
            if (caps.extensionsPerDay <= 0 || caps.extraMinutesPerDay <= 0) {
                return@withContext OverrideResult.Denied(OverrideDenialReason.DISABLED)
            }

            // Take the worse of the rule's ledger and the event log. The ledger is authoritative
            // for today; the log is the backstop that survives the rule being deleted and re-added.
            val usedCount = maxOf(
                rule.overridesUsedOn(dayStart),
                appLimitDao.countOverridesOn(dayStart, packageName),
            )
            val usedMinutes = maxOf(
                rule.overrideMinutesUsedOn(dayStart),
                appLimitDao.sumOverrideMinutesOn(dayStart, packageName),
            )

            if (usedCount >= caps.extensionsPerDay) {
                return@withContext OverrideResult.Denied(OverrideDenialReason.EXTENSIONS_EXHAUSTED)
            }
            val minutesBudget = caps.extraMinutesPerDay - usedMinutes
            if (minutesBudget <= 0) {
                return@withContext OverrideResult.Denied(OverrideDenialReason.MINUTES_EXHAUSTED)
            }

            // Grant no more than the day's remaining budget, and never more than one extension is
            // allowed to be worth. Clamping here (rather than trusting the caller's `minutes`) is
            // what stops any call site from asking for an arbitrary amount of time.
            val granted = minutes
                .coerceAtMost(minutesBudget)
                .coerceAtMost(AppLimitCeilings.MAX_MINUTES_PER_EXTENSION)
            if (granted <= 0) {
                return@withContext OverrideResult.Denied(OverrideDenialReason.MINUTES_EXHAUSTED)
            }

            // Extend from the later of now and the current window, so overlapping grants don't
            // silently discard time — but the total is still bounded by the minute budget above.
            val windowStart = maxOf(now, rule.overrideUntilEpochMs)
            val overrideUntil = windowStart + granted * 60_000L

            // The conditional UPDATE re-checks the budget against the row itself. A zero row count
            // means another caller spent it first, and we must refuse rather than retry.
            val spent = appLimitDao.spendOverrideBudget(
                packageName = packageName,
                minutes = granted,
                overrideUntilEpochMs = overrideUntil,
                nowEpochMs = now,
                dayStartEpochMs = dayStart,
                maxExtensions = caps.extensionsPerDay,
                maxExtraMinutes = caps.extraMinutesPerDay,
            )
            if (spent == 0) {
                return@withContext OverrideResult.Denied(OverrideDenialReason.EXTENSIONS_EXHAUSTED)
            }

            // The user bought themselves time — clear the "you're done" notification. The stage was
            // reset to NONE inside the same UPDATE, so the countdown can speak again when the
            // extension runs out.
            notifications.clear(packageName)

            appLimitDao.upsertEvent(
                AppLimitEvent(
                    packageName = packageName,
                    label = runCatching { appRepository.getApp(packageName)?.label ?: packageName }
                        .getOrNull() ?: packageName,
                    eventType = AppLimitEventType.OVERRIDE,
                    timestampEpochMs = now,
                    dayStartEpochMs = dayStart,
                    limitMinutes = rule.dailyLimitMinutes,
                    usedMinutes = (usageStatsTracker.todayForegroundFor(packageName) / 60_000L).toInt(),
                    overrideMinutes = granted,
                ).toEntity(),
            )
            OverrideResult.Granted(grantedMinutes = granted, untilEpochMs = overrideUntil)
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

        val status = statusFor(rule, label, usedMs, now, blockedToday = true)
        recordBlockedLaunch(status)
        refreshUsageSnapshot()
        AppLimitDecision.Blocked(status)
    }

    override suspend fun statusFor(packageName: String, label: String): AppLimitStatus? =
        withContext(dispatcher) {
            val rule = appLimitDao.getRule(packageName)?.toDomain() ?: return@withContext null
            val now = System.currentTimeMillis()
            val usedMs = runCatching { usageStatsTracker.todayForegroundFor(packageName) }
                .getOrDefault(0L)
            val blocked = rule.enabled &&
                rule.overrideUntilEpochMs <= now &&
                usedMs >= rule.dailyLimitMinutes * 60_000L
            statusFor(rule, label, usedMs, now, blockedToday = blocked)
        }

    /**
     * Build the UI-facing status for a rule, with the extension budget resolved.
     *
     * Both the count and the minute total are reported so the block screen can be specific about
     * what has run out — and so it can hide the extension button for the right reason rather than
     * offering an action that is certain to be refused.
     */
    private suspend fun statusFor(
        rule: AppLimitRule,
        label: String,
        usedMs: Long,
        nowMs: Long,
        blockedToday: Boolean,
    ): AppLimitStatus {
        val dayStart = todayDayStart(nowMs)
        val caps = activeCaps(nowMs)
        val usedCount = maxOf(
            rule.overridesUsedOn(dayStart),
            appLimitDao.countOverridesOn(dayStart, rule.packageName),
        )
        val usedMinutes = maxOf(
            rule.overrideMinutesUsedOn(dayStart),
            appLimitDao.sumOverrideMinutesOn(dayStart, rule.packageName),
        )
        return AppLimitStatus(
            packageName = rule.packageName,
            label = label,
            enabled = rule.enabled,
            dailyLimitMinutes = rule.dailyLimitMinutes,
            usedMinutes = (usedMs / 60_000L).toInt(),
            overrideUntilEpochMs = rule.overrideUntilEpochMs,
            blockedToday = blockedToday,
            overridesUsedToday = usedCount,
            overrideLimitPerDay = caps.extensionsPerDay,
            overrideMinutesUsedToday = usedMinutes,
            overrideMinutesLimitPerDay = caps.extraMinutesPerDay,
        )
    }

    override suspend fun minutesPerExtension(): Int = withContext(dispatcher) {
        settingsRepository.current().limitMinutesPerExtension
            .coerceIn(1, AppLimitCeilings.MAX_MINUTES_PER_EXTENSION)
    }

    /**
     * Change the extension caps.
     *
     * Lowering applies at once; raising is parked until the next daily reset. See
     * [activeCaps] for why that asymmetry exists — without it, the caps screen would be a slower
     * route to the very exploit the caps were added to close.
     *
     * @return true when the change took effect immediately, false when it is pending.
     */
    override suspend fun setExtensionCaps(extensionsPerDay: Int, extraMinutesPerDay: Int): Boolean =
        withContext(dispatcher) {
            val requested = AppLimitExtensionCaps(extensionsPerDay, extraMinutesPerDay).clamped()
            val settings = settingsRepository.current()
            val currentEffective = AppLimitExtensionCaps(
                extensionsPerDay = settings.limitExtensionsPerDay,
                extraMinutesPerDay = settings.limitExtraMinutesPerDay,
            ).clamped()

            val isRelaxation = requested.extensionsPerDay > currentEffective.extensionsPerDay ||
                requested.extraMinutesPerDay > currentEffective.extraMinutesPerDay

            if (!isRelaxation) {
                settingsRepository.update {
                    it.copy(
                        limitExtensionsPerDay = requested.extensionsPerDay,
                        limitExtraMinutesPerDay = requested.extraMinutesPerDay,
                        pendingLimitExtensionsPerDay = -1,
                        pendingLimitExtraMinutesPerDay = -1,
                        limitCapsPendingSinceDayStartEpochMs = 0L,
                    )
                }
                return@withContext true
            }

            settingsRepository.update {
                it.copy(
                    // Any part of the change that tightens still lands now; only the raise waits.
                    limitExtensionsPerDay = minOf(
                        it.limitExtensionsPerDay,
                        requested.extensionsPerDay,
                    ),
                    limitExtraMinutesPerDay = minOf(
                        it.limitExtraMinutesPerDay,
                        requested.extraMinutesPerDay,
                    ),
                    pendingLimitExtensionsPerDay = requested.extensionsPerDay,
                    pendingLimitExtraMinutesPerDay = requested.extraMinutesPerDay,
                    limitCapsPendingSinceDayStartEpochMs = todayDayStart(),
                )
            }
            false
        }

    override suspend fun currentExtensionCaps(): AppLimitExtensionCaps =
        withContext(dispatcher) { activeCaps(System.currentTimeMillis()) }

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
