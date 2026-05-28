package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val pinHash: String? = null,
    val pinProtected: Boolean,
    val grayscaleForced: Boolean,
    val kioskModeEnabled: Boolean,
    val hiddenStatusBar: Boolean = false,
    val focusModeEnabled: Boolean = false,
    val preferencesBlob: String = ""
)
