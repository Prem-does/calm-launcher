package com.calmlauncher.data.repository

import com.calmlauncher.data.db.AnalyticsDao
import com.calmlauncher.data.db.entity.AppUsageEntity
import com.calmlauncher.data.db.entity.DailyUsageEntity
import com.calmlauncher.data.db.entity.NotificationEventEntity
import com.calmlauncher.data.db.entity.SessionEntity
import com.calmlauncher.data.db.entity.UnlockEntity
import com.calmlauncher.data.system.UsageStatsTracker
import com.calmlauncher.di.IoDispatcher
import com.calmlauncher.domain.model.AnalyticsDashboardSnapshot
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppUsageRecord
import com.calmlauncher.domain.model.DailyUsageRecord
import com.calmlauncher.domain.model.NotificationEventType
import com.calmlauncher.domain.model.NotificationRecord
import com.calmlauncher.domain.model.UnlockRecord
import com.calmlauncher.domain.model.UsageSessionRecord
import com.calmlauncher.domain.model.UsageSortOrder
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.AnalyticsRepository
import com.calmlauncher.domain.repository.LaunchEventRepository
import com.calmlauncher.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val appRepository: AppRepository,
    private val launchEventRepository: LaunchEventRepository,
    private val settingsRepository: SettingsRepository,
    private val usageStatsTracker: UsageStatsTracker,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : AnalyticsRepository {

    override fun hasUsageAccess(): Boolean = usageStatsTracker.hasPermission()

    override fun observeDashboard(days: Int): Flow<AnalyticsDashboardSnapshot> = combine(
        observeDailyUsage(days),
        observeAppUsage(days, UsageSortOrder.MOST_USED),
        observeSessions(days),
        observeUnlocks(days),
        observeNotifications(days),
    ) { daily, apps, sessions, unlocks, notifications ->
        // Match on the actual day key rather than taking the last row. Before the first
        // refresh of a new day there is no row for today yet, and "last row" would quietly
        // present yesterday's total as today's.
        val todayStart = startOfToday()
        val yesterdayStart = startOfDaysAgo(1)
        val today = daily.firstOrNull { it.dayStartEpochMs == todayStart } ?: emptyDaily(todayStart)
        val yesterday = daily.firstOrNull { it.dayStartEpochMs == yesterdayStart }
            ?: emptyDaily(yesterdayStart)
        AnalyticsDashboardSnapshot(
            today = today,
            yesterday = yesterday,
            dailyHistory = daily,
            appUsage = apps,
            sessions = sessions,
            unlocks = unlocks,
            notifications = notifications,
        )
    }.flowOn(dispatcher)

    /**
     * A contiguous day-by-day series, oldest first. Days with no stored row are emitted as
     * zeroes rather than omitted: a chart that silently drops a day renders five bars for a
     * week and misaligns every label, which reads as a data error rather than a quiet day.
     */
    override fun observeDailyUsage(days: Int): Flow<List<DailyUsageRecord>> {
        val span = days.coerceAtLeast(1)
        val start = startOfDaysAgo(span - 1)
        val end = startOfToday()
        return analyticsDao.observeDailyRange(start, end)
            .map { rows ->
                val byDay = rows.associate { it.dayStartEpochMs to it.toDomain() }
                (span - 1 downTo 0).map { daysAgo ->
                    val dayStart = startOfDaysAgo(daysAgo)
                    byDay[dayStart] ?: emptyDaily(dayStart)
                }
            }
            .flowOn(dispatcher)
    }

    override fun observeAppUsage(days: Int, sortOrder: UsageSortOrder): Flow<List<AppUsageRecord>> {
        val start = startOfDaysAgo(days.coerceAtLeast(1) - 1)
        val end = startOfToday()
        return combine(
            analyticsDao.observeAppUsageRange(start, end),
            appRepository.observeApps(),
        ) { rows, currentApps ->
            val currentCategories = currentApps.associate { it.packageName to it.category }
            val currentLabels = currentApps.associate { it.packageName to it.label }
            rows.map { row ->
                val record = row.toDomain()
                // Re-resolve name and category from the live catalog so rows written before
                // labels were available (or before a re-categorisation) still read correctly.
                record.copy(
                    appName = currentLabels[record.packageName] ?: record.appName,
                    category = currentCategories[record.packageName] ?: record.category,
                )
            }.sortedWith(sortOrder.comparator())
        }
            .flowOn(dispatcher)
    }

    override fun observeSessions(days: Int): Flow<List<UsageSessionRecord>> {
        val start = startOfDaysAgo(days.coerceAtLeast(1) - 1)
        val end = startOfToday()
        return analyticsDao.observeSessionsRange(start, end).map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    override fun observeUnlocks(days: Int): Flow<List<UnlockRecord>> {
        val start = startOfDaysAgo(days.coerceAtLeast(1) - 1)
        val end = startOfToday()
        return analyticsDao.observeUnlocksRange(start, end).map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    override fun observeNotifications(days: Int): Flow<List<NotificationRecord>> {
        val start = startOfDaysAgo(days.coerceAtLeast(1) - 1)
        val end = startOfToday()
        return analyticsDao.observeNotificationsRange(start, end)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    /**
     * Rebuild the stored history from the system's event log.
     *
     * This deliberately rewrites a whole window of days rather than only today. Android keeps
     * its raw usage events for days, but this app only runs occasionally — writing just
     * "today" meant any day the launcher wasn't opened was permanently recorded as zero, and
     * the weekly chart was mostly holes. Re-deriving [BACKFILL_DAYS] of history from the
     * source of truth on every refresh makes the chart correct after a gap, and makes it
     * self-healing if a day was ever written wrong.
     *
     * Every write is an upsert keyed by day, and sessions for a rewritten day are cleared
     * first, so repeated refreshes converge instead of accumulating duplicates.
     */
    override suspend fun refresh() = withContext(dispatcher) {
        val settings = settingsRepository.current()
        if (!settings.collectUsageAnalyticsEnabled) return@withContext
        if (!usageStatsTracker.hasPermission()) return@withContext

        val now = System.currentTimeMillis()
        // Never re-derive further back than the user's own retention setting allows.
        val windowDays = BACKFILL_DAYS.coerceAtMost(settings.analyticsRetentionDays.coerceAtLeast(1))
        val windowStart = startOfDaysAgo(windowDays - 1)

        val installedApps = appRepository.observeApps().first()
        val appCategories = installedApps.associate { it.packageName to it.category }
        // Real PackageManager labels. Deriving a name from the package id turns
        // "com.google.android.youtube" into "Youtube" and "com.samsung.android.app.notes"
        // into "Notes" — close enough to look right and wrong often enough to confuse.
        val appLabels = installedApps.associate { it.packageName to it.label }

        val launchesByDay = launchEventRepository.since(windowStart)
            .groupBy { startOfToday(it.timestampEpochMs) }

        usageStatsTracker.rangeUsage(windowStart, now).forEach { day ->
            val record = day.record
            val sessions = day.sessions
            val dayStart = record.dayStartEpochMs
            val launches = launchesByDay[dayStart].orEmpty()

            analyticsDao.upsertDaily(
                DailyUsageEntity(
                    dayStartEpochMs = dayStart,
                    totalScreenTimeMinutes = record.totalMinutes.toInt(),
                    unlockCount = analyticsDao.unlockCountForDay(dayStart),
                    notificationCount = analyticsDao.notificationCountForDay(dayStart),
                    longestSessionMinutes = sessions.maxOfOrNull { it.durationMinutes } ?: 0,
                    appLaunchCount = launches.size,
                    updatedAtEpochMs = now,
                ),
            )

            val launchCounts = launches.groupingBy { it.packageName }.eachCount()
            // Apps that used to have a row for this day but no longer register any usage
            // would otherwise keep their stale minutes forever.
            analyticsDao.deleteAppsForDay(dayStart)
            analyticsDao.upsertApps(
                record.perApp.entries.map { (packageName, millis) ->
                    AppUsageEntity(
                        dayStartEpochMs = dayStart,
                        packageName = packageName,
                        appName = appLabels[packageName] ?: friendlyName(packageName),
                        category = appCategories[packageName]?.name ?: inferCategory(packageName).name,
                        usageMinutes = (millis / 60_000L).toInt(),
                        launchCount = launchCounts[packageName] ?: 0,
                        updatedAtEpochMs = now,
                    )
                },
            )

            analyticsDao.deleteSessionsForDay(dayStart)
            analyticsDao.insertSessions(
                sessions.map {
                    SessionEntity(
                        dayStartEpochMs = dayStart,
                        packageName = it.packageName,
                        appName = appLabels[it.packageName] ?: it.appName,
                        startTimeEpochMs = it.startTimeEpochMs,
                        endTimeEpochMs = it.endTimeEpochMs,
                        durationMinutes = it.durationMinutes,
                    )
                },
            )
        }

        pruneOlderThan(settings.analyticsRetentionDays)
    }

    override suspend fun recordUnlock(timestampEpochMs: Long) = withContext(dispatcher) {
        analyticsDao.insertUnlock(
            UnlockEntity(
                timestampEpochMs = timestampEpochMs,
                dayStartEpochMs = startOfToday(timestampEpochMs),
            ),
        )
    }

    override suspend fun recordNotification(
        packageName: String,
        timestampEpochMs: Long,
        eventType: NotificationEventType,
    ) = withContext(dispatcher) {
        analyticsDao.insertNotification(
            NotificationEventEntity(
                timestampEpochMs = timestampEpochMs,
                dayStartEpochMs = startOfToday(timestampEpochMs),
                packageName = packageName,
                eventType = eventType.name,
            ),
        )
    }

    override suspend fun clearHistory() = withContext(dispatcher) {
        analyticsDao.deleteAllDaily()
        analyticsDao.deleteAllApps()
        analyticsDao.deleteAllSessions()
        analyticsDao.deleteAllUnlocks()
        analyticsDao.deleteAllNotifications()
    }

    override suspend fun pruneOlderThan(days: Int) = withContext(dispatcher) {
        if (days <= 0) return@withContext
        val before = startOfDaysAgo(days)
        analyticsDao.deleteDailyBefore(before)
        analyticsDao.deleteAppsBefore(before)
        analyticsDao.deleteSessionsBefore(before)
        analyticsDao.deleteUnlocksBefore(before)
        analyticsDao.deleteNotificationsBefore(before)
    }

    /** Last-resort name for a package the launcher catalog doesn't know about. */
    private fun friendlyName(packageName: String): String {
        val candidate = packageName
            .substringBefore("/")
            .split('.')
            .lastOrNull { it.isNotBlank() && it != "android" && it != "app" }
            ?: packageName
        return candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun inferCategory(packageName: String): AppCategory {
        val value = packageName.lowercase()
        return when {
            value.contains("instagram") || value.contains("facebook") || value.contains("snapchat") || value.contains("tiktok") || value.contains("twitter") || value.contains("x.") -> AppCategory.SOCIAL
            value.contains("youtube") || value.contains("netflix") || value.contains("primevideo") || value.contains("disney") || value.contains("spotify") -> AppCategory.ENTERTAINMENT
            value.contains("whatsapp") || value.contains("telegram") || value.contains("messages") || value.contains("discord") || value.contains("messenger") -> AppCategory.COMMUNICATION
            value.contains("chrome") || value.contains("browser") || value.contains("firefox") || value.contains("edge") -> AppCategory.BROWSER
            value.contains("play") || value.contains("store") || value.contains("market") -> AppCategory.STORE
            value.contains("game") || value.contains("gaming") -> AppCategory.GAME
            value.contains("calendar") || value.contains("docs") || value.contains("drive") || value.contains("sheets") || value.contains("slides") || value.contains("notion") || value.contains("keep") -> AppCategory.TOOL
            else -> AppCategory.OTHER
        }
    }

    private fun startOfToday(nowEpochMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowEpochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun startOfDaysAgo(daysAgo: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun emptyDaily(dayStart: Long) = DailyUsageRecord(dayStart, 0, 0, 0, 0, 0)

    private fun com.calmlauncher.data.db.entity.DailyUsageEntity.toDomain() = DailyUsageRecord(
        dayStartEpochMs = dayStartEpochMs,
        totalScreenTimeMinutes = totalScreenTimeMinutes,
        unlockCount = unlockCount,
        notificationCount = notificationCount,
        longestSessionMinutes = longestSessionMinutes,
        appLaunchCount = appLaunchCount,
    )

    private fun AppUsageEntity.toDomain() = AppUsageRecord(
        dayStartEpochMs = dayStartEpochMs,
        packageName = packageName,
        appName = appName,
        category = category.toAppCategory(),
        usageMinutes = usageMinutes,
        launchCount = launchCount,
    )

    private fun String?.toAppCategory(): AppCategory =
        AppCategory.entries.firstOrNull { it.name == this } ?: when (this) {
            "VIDEO" -> AppCategory.ENTERTAINMENT
            "PRODUCTIVITY" -> AppCategory.TOOL
            else -> AppCategory.OTHER
        }

    private fun SessionEntity.toDomain() = UsageSessionRecord(
        dayStartEpochMs = dayStartEpochMs,
        packageName = packageName,
        appName = appName,
        startTimeEpochMs = startTimeEpochMs,
        endTimeEpochMs = endTimeEpochMs,
        durationMinutes = durationMinutes,
    )

    private fun UnlockEntity.toDomain() = UnlockRecord(
        timestampEpochMs = timestampEpochMs,
        dayStartEpochMs = dayStartEpochMs,
    )

    private fun NotificationEventEntity.toDomain() = NotificationRecord(
        timestampEpochMs = timestampEpochMs,
        dayStartEpochMs = dayStartEpochMs,
        packageName = packageName,
        eventType = runCatching { NotificationEventType.valueOf(eventType) }.getOrDefault(NotificationEventType.REMOVED),
    )

    private fun UsageSortOrder.comparator(): Comparator<AppUsageRecord> = when (this) {
        UsageSortOrder.MOST_USED -> compareByDescending<AppUsageRecord> { it.usageMinutes }.thenBy { it.appName }
        UsageSortOrder.LEAST_USED -> compareBy<AppUsageRecord> { it.usageMinutes }.thenBy { it.appName }
        UsageSortOrder.ALPHABETICAL -> compareBy<AppUsageRecord> { it.appName }
    }

    private companion object {
        /**
         * How many days of history to re-derive on each refresh. Android's own event log is
         * the limit here; a week comfortably covers the trend chart and any gap where the
         * launcher simply wasn't opened.
         */
        const val BACKFILL_DAYS = 7
    }
}
