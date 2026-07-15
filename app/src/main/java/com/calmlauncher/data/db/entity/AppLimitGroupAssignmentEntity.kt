package com.calmlauncher.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "app_limit_group_assignments",
    primaryKeys = ["groupId", "packageName"],
)
data class AppLimitGroupAssignmentEntity(
    val groupId: String,
    val packageName: String,
    val updatedAtEpochMs: Long,
)
