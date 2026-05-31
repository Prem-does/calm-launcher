package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_event")
data class UnlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampEpochMs: Long,
    val dayStartEpochMs: Long,
)
