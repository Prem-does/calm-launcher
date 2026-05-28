package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calmlauncher.data.db.entity.TelemetryEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TelemetryEvent)

    @Query("SELECT * FROM telemetry_events ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<TelemetryEvent>>
}
