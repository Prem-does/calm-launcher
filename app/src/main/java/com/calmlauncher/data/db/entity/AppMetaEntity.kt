package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.calmlauncher.domain.model.AppCategory

/**
 * Per-app launcher metadata. Labels and the existence of the package come live from
 * PackageManager — only the launcher-specific overrides are persisted here.
 *
 * [isDistractingOverride] is nullable: null means "fall back to the category default"
 * ([AppCategory.isDistractingByDefault]); a non-null value is an explicit user override.
 */
@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val packageName: String,
    val category: String = AppCategory.OTHER.name,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = -1,
    val isDistractingOverride: Boolean? = null,
)
