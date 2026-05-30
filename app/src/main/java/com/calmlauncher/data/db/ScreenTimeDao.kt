package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.calmlauncher.data.db.entity.ScreenTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenTimeDao {

    @Upsert
    suspend fun upsert(entity: ScreenTimeEntity)

    @Query("SELECT * FROM screen_time WHERE dayStartEpochMs = :dayStartEpochMs LIMIT 1")
    fun observe(dayStartEpochMs: Long): Flow<ScreenTimeEntity?>

    @Query("SELECT * FROM screen_time WHERE dayStartEpochMs = :dayStartEpochMs LIMIT 1")
    suspend fun get(dayStartEpochMs: Long): ScreenTimeEntity?

    @Query("SELECT * FROM screen_time WHERE dayStartEpochMs >= :startEpochMs AND dayStartEpochMs <= :endEpochMs ORDER BY dayStartEpochMs ASC")
    fun observeRange(startEpochMs: Long, endEpochMs: Long): Flow<List<ScreenTimeEntity>>
}
