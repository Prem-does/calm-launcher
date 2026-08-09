package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "routine_completions",
    primaryKeys = ["taskId", "dayStartEpochMs"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dayStartEpochMs")],
)
data class RoutineCompletionEntity(
    val taskId: Long,
    val dayStartEpochMs: Long,
    val completed: Boolean,
    val actualValue: Int?,
    val timestampEpochMs: Long,
)