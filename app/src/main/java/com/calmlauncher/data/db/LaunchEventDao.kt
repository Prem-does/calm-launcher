package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.calmlauncher.data.db.entity.LaunchEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LaunchEventDao {

    @Insert
    suspend fun insert(event: LaunchEventEntity): Long

    @Query("SELECT * FROM launch_event WHERE timestampEpochMs >= :sinceEpochMs ORDER BY timestampEpochMs DESC")
    fun observeSince(sinceEpochMs: Long): Flow<List<LaunchEventEntity>>

    @Query("SELECT * FROM launch_event WHERE timestampEpochMs >= :sinceEpochMs ORDER BY timestampEpochMs DESC")
    suspend fun since(sinceEpochMs: Long): List<LaunchEventEntity>

    @Query("DELETE FROM launch_event WHERE timestampEpochMs < :beforeEpochMs")
    suspend fun deleteOlderThan(beforeEpochMs: Long)
}
