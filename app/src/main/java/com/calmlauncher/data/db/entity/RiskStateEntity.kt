package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted Dopamine Detection Engine output. Singleton row: always [SINGLETON_ID].
 * Mirrors [com.calmlauncher.domain.model.RiskState].
 */
@Entity(tableName = "risk_state")
data class RiskStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val tier: String,
    val score: Int,
    val repeatedOpens: Int,
    val rapidSwitches: Int,
    val lateNightLaunches: Int,
    val longestSessionMs: Long,
    val updatedAtEpochMs: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
