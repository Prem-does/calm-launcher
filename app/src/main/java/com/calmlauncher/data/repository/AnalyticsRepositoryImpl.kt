package com.calmlauncher.data.repository

import com.calmlauncher.data.db.AnalyticsDao
import com.calmlauncher.data.db.entity.AppUsageEntity
import com.calmlauncher.data.db.entity.DailyUsageEntity
import com.calmlauncher.data.db.entity.NotificationEventEntity
import com.calmlauncher.data.db.entity.SessionEntity
import com.calmlauncher.data.db.entity.UnlockEntity
import com.calmlauncher.data.system.UsageStatsTracker
import com.calmlauncher.di.IoDispatcher
import com.calmlauncher.domain.model.AnalyticsCategory
import com.calmlauncher.domain.model.AnalyticsDashboardSnapshot
import com.calmlauncher.domain.model.AppUsageRecord
import com.calmlauncher.domain.model.DailyUsageRecord
import com.calmlauncher.domain.model.NotificationEventType
import com.calmlauncher.domain.model.NotificationRecord
import com.calmlauncher.domain.model.UnlockRecord
import com.calmlauncher.domain.model.UsageSessionRecord
import com.calmlauncher.domain.model.UsageSortOrder
import com.calmlauncher.domain.repository.AnalyticsRepository
import com.calmlauncher.domain.repository.LaunchEventRepository
import com.calmlauncher.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val launchEventRepository: LaunchEventRepository,
    private val settingsRepository: SettingsRepository,
    private val usageStatsTracker: UsageStatsTracker,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : AnalyticsRepository {

    override fun observeDashboard(days: Int): Flow<AnalyticsDashboardSnapshot> = combine(
        observeDailyUsage(days),
        observeAppUsage(days, UsageSortOrder.MOST_USED),
        observeSessions(days),
        observeUnlocks(days),
        observeNotifications(days),
    ) { daily, apps, sessions, unlocks, notifications ->
        val today = daily.lastOrNull() ?: emptyDaily(startOfToday())
        val yesterday = daily.dropLast(1).lastOrNull() ?: emptyDaily(startOfToday() - DAY_MS)
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

    override fun observeDailyUsage(days: Int): Flow<List<DailyUsageRecord>> {
        val start = startOfDaysAgo(days.coerceAtLeast(1) - 1)
        val end = startOfToday()
        return analyticsDao.observeDailyRange(start, end).map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    override fun observeAppUsage(days: Int, sortOrder: UsageSortOrder): Flow<List<AppUsageRecord>> {
        val start = startOfDaysAgo(days.coerceAtLeast(1) - 1)
        val end = startOfToday()
        return analyticsDao.observeAppUsageRange(start, end)
            .map { rows -> rows.map { it.toDomain() }.sortedWith(sortOrder.comparator()) }
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

    override suspend fun refresh() = withContext(dispatcher) {
        val settings = settingsRepository.current()
        if (!settings.collectUsageAnalyticsEnabled) return@withContext

        val now = System.currentTimeMillis()
        val dayStart = startOfToday(now)
        val record = usageStatsTracker.todayForeground()
        val sessions = usageStatsTracker.todaySessions()
        val launches = launchEventRepository.since(dayStart)

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
        analyticsDao.upsertApps(
            record.perApp.entries.map { (packageName, millis) ->
                AppUsageEntity(
                    dayStartEpochMs = dayStart,
                    packageName = packageName,
                    appName = friendlyName(packageName),
                    category = categorize(packageName).name,
                    usageMinutes = (millis / 60_000L).toInt(),
                    launchCount = launchCounts[packageName] ?: 0,
                    updatedAtEpochMs = now,
                )
            },
        )

        analyticsDao.insertSessions(
            sessions.map {
                SessionEntity(
                    dayStartEpochMs = dayStart,
                    packageName = it.packageName,
                    appName = it.appName,
                    startTimeEpochMs = it.startTimeEpochMs,
                    endTimeEpochMs = it.endTimeEpochMs,
                    durationMinutes = it.durationMinutes,
                )
            },
        )

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

    private fun friendlyName(packageName: String): String {
        val candidate = packageName
            .substringBefore("/")
            .split('.')
            .lastOrNull { it.isNotBlank() && it != "android" && it != "app" }
            ?: packageName
        return candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun categorize(packageName: String): AnalyticsCategory {
        val value = packageName.lowercase()
        return when {
            value.contains("instagram") || value.contains("facebook") || value.contains("snapchat") || value.contains("tiktok") || value.contains("twitter") || value.contains("x.") -> AnalyticsCategory.SOCIAL
            value.contains("youtube") || value.contains("netflix") || value.contains("primevideo") || value.contains("disney") || value.contains("spotify") -> AnalyticsCategory.VIDEO
            value.contains("whatsapp") || value.contains("telegram") || value.contains("messages") || value.contains("discord") || value.contains("messenger") -> AnalyticsCategory.COMMUNICATION
            value.contains("calendar") || value.contains("docs") || value.contains("drive") || value.contains("sheets") || value.contains("slides") || value.contains("notion") || value.contains("keep") -> AnalyticsCategory.PRODUCTIVITY
            else -> AnalyticsCategory.OTHER
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
        category = runCatching { AnalyticsCategory.valueOf(category) }.getOrDefault(AnalyticsCategory.OTHER),
        usageMinutes = usageMinutes,
        launchCount = launchCount,
    )

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
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
