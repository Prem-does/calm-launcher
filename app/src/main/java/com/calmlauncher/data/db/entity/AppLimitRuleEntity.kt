package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limit_rules")
data class AppLimitRuleEntity(
    @PrimaryKey val packageName: String,
    val enabled: Boolean,
    val dailyLimitMinutes: Int,
    val overrideUntilEpochMs: Long,
    val updatedAtEpochMs: Long,
    val lastNotifiedEpochMs: Long = 0L,
    /** Name of the last [com.calmlauncher.domain.model.LimitNotifyStage] posted for this app. */
    val lastNotifiedStage: String = "NONE",
    /** Day-start the stage belongs to, so it resets cleanly at midnight. */
    val lastNotifiedDayStartEpochMs: Long = 0L,

    /**
     * The extension ledger, and the day it belongs to.
     *
     * These three columns exist because the extension budget used to be *derived* by counting
     * OVERRIDE rows in `app_limit_events`, which made it easy to reset by accident: saving a
     * group limit rebuilt the rule from scratch, and any code path that rewrote the rule
     * silently handed back a fresh allowance. Keeping the ledger on the rule means the check and
     * the spend happen against the same row, in the same transaction as the grant.
     *
     * [overrideDayStartEpochMs] is the reset mechanism: a value that isn't today's day-start
     * means the counters below describe a previous day and are read as zero. Nothing has to run
     * at midnight for the budget to roll over.
     */
    val overrideDayStartEpochMs: Long = 0L,

    /** Extensions granted on [overrideDayStartEpochMs]. */
    val overridesUsedToday: Int = 0,

    /** Total extra minutes granted on [overrideDayStartEpochMs]. */
    val overrideMinutesUsedToday: Int = 0,
)
