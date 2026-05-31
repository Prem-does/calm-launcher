package com.calmlauncher.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "app_usage",
    primaryKeys = ["dayStartEpochMs", "packageName"],
)
data class AppUsageEntity(
    val dayStartEpochMs: Long,
    val packageName: String,
    val appName: String,
    val category: String,
    val usageMinutes: Int,
    val launchCount: Int,
    val updatedAtEpochMs: Long,
)
