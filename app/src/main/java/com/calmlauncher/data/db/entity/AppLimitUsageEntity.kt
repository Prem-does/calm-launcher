package com.calmlauncher.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "app_limit_usage",
    primaryKeys = ["dayStartEpochMs", "packageName"],
)
data class AppLimitUsageEntity(
    val dayStartEpochMs: Long,
    val packageName: String,
    val usedMs: Long,
    val lastSyncedAtEpochMs: Long,
)
