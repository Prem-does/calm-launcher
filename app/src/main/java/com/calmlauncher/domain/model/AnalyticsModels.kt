package com.calmlauncher.domain.model

enum class AnalyticsRange { TODAY, SEVEN_DAYS, THIRTY_DAYS, NINETY_DAYS, YEAR }

enum class UsageSortOrder { MOST_USED, LEAST_USED, ALPHABETICAL }

enum class NotificationEventType { POSTED, REMOVED, OPENED, IGNORED }

data class UsageSessionRecord(
    val dayStartEpochMs: Long,
    val packageName: String,
    val appName: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val durationMinutes: Int,
)

data class AppUsageRecord(
    val dayStartEpochMs: Long,
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val usageMinutes: Int,
    val launchCount: Int,
)

data class DailyUsageRecord(
    val dayStartEpochMs: Long,
    val totalScreenTimeMinutes: Int,
    val unlockCount: Int,
    val notificationCount: Int,
    val longestSessionMinutes: Int,
    val appLaunchCount: Int,
)

data class UnlockRecord(
    val timestampEpochMs: Long,
    val dayStartEpochMs: Long,
)

data class NotificationRecord(
    val timestampEpochMs: Long,
    val dayStartEpochMs: Long,
    val packageName: String,
    val eventType: NotificationEventType,
)

data class AnalyticsDashboardSnapshot(
    val today: DailyUsageRecord,
    val yesterday: DailyUsageRecord,
    val dailyHistory: List<DailyUsageRecord>,
    val appUsage: List<AppUsageRecord>,
    val sessions: List<UsageSessionRecord>,
    val unlocks: List<UnlockRecord>,
    val notifications: List<NotificationRecord>,
)
