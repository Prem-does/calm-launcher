package com.calmlauncher.domain.model

/** Persistent per-app limit rule configured by the user. */
data class AppLimitRule(
    val packageName: String,
    val enabled: Boolean = false,
    val dailyLimitMinutes: Int = 30,
    val overrideUntilEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    val lastNotifiedEpochMs: Long = 0L,
)

/** Daily foreground usage snapshot for one app. */
data class AppLimitUsage(
    val dayStartEpochMs: Long,
    val packageName: String,
    val usedMs: Long,
    val lastSyncedAtEpochMs: Long,
)

enum class AppLimitEventType {
    BLOCKED,
    OVERRIDE,
}

/** Append-only event log for app-limit hits and overrides. */
data class AppLimitEvent(
    val id: Long = 0L,
    val packageName: String,
    val label: String,
    val eventType: AppLimitEventType,
    val timestampEpochMs: Long,
    val dayStartEpochMs: Long,
    val limitMinutes: Int,
    val usedMinutes: Int,
    val overrideMinutes: Int = 0,
)

/** UI-facing status for a single app in the App Limits screen. */
data class AppLimitStatus(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val dailyLimitMinutes: Int?,
    val usedMinutes: Int,
    val overrideUntilEpochMs: Long,
    val blockedToday: Boolean,
) {
    val remainingMinutes: Int?
        get() = dailyLimitMinutes?.let { limit -> (limit - usedMinutes).coerceAtLeast(0) }
}

/** Dashboard summary surfaced in Settings/Reflection. */
data class AppLimitSummary(
    val blockedLaunchesToday: Int = 0,
    val limitedAppsToday: Int = 0,
    val estimatedTimeSavedMinutes: Int = 0,
    val topLimitedPackage: String? = null,
    val topLimitedCount: Int = 0,
)

sealed interface AppLimitDecision {
    data object Allowed : AppLimitDecision
    data class Blocked(val status: AppLimitStatus) : AppLimitDecision
}
