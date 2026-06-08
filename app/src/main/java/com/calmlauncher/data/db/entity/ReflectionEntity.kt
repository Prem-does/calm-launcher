package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Nightly reflection prompt + optional response. Mirrors [com.calmlauncher.domain.model.ReflectionEntry]. */
@Entity(tableName = "reflection")
data class ReflectionEntity(
    @PrimaryKey val dayStartEpochMs: Long,
    val prompt: String,
    val response: String? = null,
    val createdAtEpochMs: Long,
)
