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

/**
 * Hard ceilings on the app-limit extension system, above which no configuration can go.
 *
 * The caps themselves are user-configurable, which creates an obvious hole: if raising the cap
 * were enough to keep going, "how many extensions may I have" would just be another button to
 * press while blocked. Two things close it — these absolute ceilings, which the settings UI cannot
 * exceed, and the rule that a *raised* cap only takes effect at the next daily reset.
 */
object AppLimitCeilings {
    /** No more than this many extensions per app per day, whatever the user configures. */
    const val MAX_EXTENSIONS_PER_DAY = 3

    /** No more than this much extra time per app per day, whatever the user configures. */
    const val MAX_EXTRA_MINUTES_PER_DAY = 60

    /** The largest single extension that can be granted. */
    const val MAX_MINUTES_PER_EXTENSION = 15
}

/**
 * The extension budget in force for one app today.
 *
 * @param extensionsPerDay how many separate extensions may be granted.
 * @param extraMinutesPerDay total extra time available across those extensions.
 */
data class AppLimitExtensionCaps(
    val extensionsPerDay: Int,
    val extraMinutesPerDay: Int,
) {
    /** Clamp to the absolute ceilings. Applied on read as well as on write, so a value written by
     *  an older build (or edited on disk) can't exceed them either. */
    fun clamped(): AppLimitExtensionCaps = AppLimitExtensionCaps(
        extensionsPerDay = extensionsPerDay.coerceIn(0, AppLimitCeilings.MAX_EXTENSIONS_PER_DAY),
        extraMinutesPerDay = extraMinutesPerDay
            .coerceIn(0, AppLimitCeilings.MAX_EXTRA_MINUTES_PER_DAY),
    )
}

/** Why an extension was refused. Surfaced so the block screen can say something true. */
enum class OverrideDenialReason {
    /** No limit rule for this app, so there is nothing to extend. */
    NO_RULE,

    /** Today's count of extensions is used up. */
    EXTENSIONS_EXHAUSTED,

    /** Today's extra-minutes budget is used up. */
    MINUTES_EXHAUSTED,

    /** Extensions are switched off entirely (cap configured to zero). */
    DISABLED,
}

/** The outcome of asking for more time. */
sealed interface OverrideResult {
    /**
     * @param grantedMinutes minutes actually added, which may be fewer than requested when the
     *   remaining budget doesn't cover the full amount. Granting a partial extension is
     *   deliberate: it spends the budget down to exactly zero rather than leaving an unusable
     *   remainder, so the last extension of the day is honest about being the last.
     * @param untilEpochMs when the app becomes blocked again.
     */
    data class Granted(val grantedMinutes: Int, val untilEpochMs: Long) : OverrideResult

    data class Denied(val reason: OverrideDenialReason) : OverrideResult
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
    /** Day the extension ledger below belongs to. A different day means the ledger reads as zero. */
    val overrideDayStartEpochMs: Long = 0L,
    val overridesUsedToday: Int = 0,
    val overrideMinutesUsedToday: Int = 0,
) {
    /** Extensions spent on [dayStartEpochMs], treating a stale ledger as zero. */
    fun overridesUsedOn(dayStartEpochMs: Long): Int =
        if (overrideDayStartEpochMs == dayStartEpochMs) overridesUsedToday else 0

    /** Extra minutes spent on [dayStartEpochMs], treating a stale ledger as zero. */
    fun overrideMinutesUsedOn(dayStartEpochMs: Long): Int =
        if (overrideDayStartEpochMs == dayStartEpochMs) overrideMinutesUsedToday else 0
}

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
    /** Extra minutes already spent on this app today. */
    val overrideMinutesUsedToday: Int = 0,
    /** Total extra minutes allowed today. */
    val overrideMinutesLimitPerDay: Int = 20,
) {
    val remainingMinutes: Int?
        get() = dailyLimitMinutes?.let { limit -> (limit - usedMinutes).coerceAtLeast(0) }

    /** Extensions still available today. */
    val overridesRemaining: Int
        get() = (overrideLimitPerDay - overridesUsedToday).coerceAtLeast(0)

    /** Extra minutes still available today. */
    val overrideMinutesRemaining: Int
        get() = (overrideMinutesLimitPerDay - overrideMinutesUsedToday).coerceAtLeast(0)

    /**
     * Whether another extension can be granted. Both budgets have to have room: running out of
     * *either* the count or the minutes ends extensions for the day, which is what stops a
     * generous count from being spent in tiny slices (or a generous minute budget from being taken
     * in one unlimited-feeling chunk).
     */
    val canGrantOverride: Boolean
        get() = overridesRemaining > 0 && overrideMinutesRemaining > 0

    /** Plain-language reason the extension button is absent, for the block screen's footnote. */
    val overrideExhaustedReason: String?
        get() = when {
            canGrantOverride -> null
            overrideLimitPerDay == 0 -> "Extensions are turned off for this app."
            overridesRemaining <= 0 && overrideLimitPerDay == 1 ->
                "Today's one extension is used up."
            overridesRemaining <= 0 ->
                "All $overrideLimitPerDay extensions are used up for today."
            else -> "Today's $overrideMinutesLimitPerDay extra minutes are used up."
        }
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
