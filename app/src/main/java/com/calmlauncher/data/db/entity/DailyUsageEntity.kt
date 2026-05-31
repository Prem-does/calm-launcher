package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey val dayStartEpochMs: Long,
    val totalScreenTimeMinutes: Int,
    val unlockCount: Int,
    val notificationCount: Int,
    val longestSessionMinutes: Int,
    val appLaunchCount: Int,
    val updatedAtEpochMs: Long,
)
