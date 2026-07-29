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
)
