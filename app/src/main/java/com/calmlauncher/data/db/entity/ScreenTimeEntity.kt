package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time")
data class ScreenTimeEntity(
    @PrimaryKey val dayKey: String,
    val minutesUsed: Int
)
