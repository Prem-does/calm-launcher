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
)
