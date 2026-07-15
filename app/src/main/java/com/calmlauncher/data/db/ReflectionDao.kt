package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.calmlauncher.data.db.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {

    @Insert
    suspend fun insert(entity: ReflectionEntity)

    @Query("SELECT * FROM reflection ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ReflectionEntity>>

    @Query("SELECT * FROM reflection WHERE dayStartEpochMs = :dayStartEpochMs ORDER BY createdAtEpochMs DESC LIMIT 1")
    suspend fun latestFor(dayStartEpochMs: Long): ReflectionEntity?
}
