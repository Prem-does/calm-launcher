package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calmlauncher.data.db.entity.LaunchEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LaunchEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: LaunchEventEntity)

    @Query("SELECT * FROM launch_events ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecentLaunches(limit: Int = 50): Flow<List<LaunchEventEntity>>
}