package com.calmlauncher.domain.model

/**
 * How far along the countdown a package's notification has already been taken today.
 *
 * The stage — not a timestamp — is what makes notifications idempotent. Two independent
 * sources fire for the same app (the exact alarm at each threshold, and the 15-minute usage
 * rollup), and without a recorded stage they each post their own copy of "5 minutes left".
 * Ordering matters: a sync only ever moves the stage *forward*, so re-running it is silent.
 */
enum class LimitNotifyStage {
    /** Nothing said yet today, or the app is comfortably under its limit. */
    NONE,
    TEN_MINUTES,
    FIVE_MINUTES,
    ONE_MINUTE,
    REACHED,
    ;

    companion object {
        /** The stage [remainingMinutes] of allowance corresponds to. */
        fun forRemaining(remainingMinutes: Int): LimitNotifyStage = when {
            remainingMinutes <= 0 -> REACHED
            remainingMinutes <= 1 -> ONE_MINUTE
            remainingMinutes <= 5 -> FIVE_MINUTES
            remainingMinutes <= 10 -> TEN_MINUTES
            else -> NONE
        }

        fun fromName(name: String): LimitNotifyStage =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}

/** Persistent per-app limit rule configured by the user. */
data class AppLimitRule(
    val packageName: String,
    val enabled: Boolean = false,
    val dailyLimitMinutes: Int = 30,
    val overrideUntilEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    val lastNotifiedEpochMs: Long = 0L,
    val lastNotifiedStage: LimitNotifyStage = LimitNotifyStage.NONE,
    /** Day the stage belongs to; a different day resets the countdown to [LimitNotifyStage.NONE]. */
    val lastNotifiedDayStartEpochMs: Long = 0L,
)

/** User-selected app membership for a visible app-limit group. */
data class AppLimitGroupAssignment(
    val groupId: String,
    val packageName: String,
    val updatedAtEpochMs: Long,
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
    val overridesUsedToday: Int = 0,
    val overrideLimitPerDay: Int = 2,
) {
    val remainingMinutes: Int?
        get() = dailyLimitMinutes?.let { limit -> (limit - usedMinutes).coerceAtLeast(0) }

    val canGrantOverride: Boolean
        get() = overridesUsedToday < overrideLimitPerDay
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
