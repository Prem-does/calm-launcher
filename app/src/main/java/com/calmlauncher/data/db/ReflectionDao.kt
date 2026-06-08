package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.calmlauncher.data.db.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {

    @Upsert
    suspend fun upsert(entity: ReflectionEntity)

    @Query("SELECT * FROM reflection ORDER BY dayStartEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ReflectionEntity>>

    @Query("SELECT * FROM reflection WHERE dayStartEpochMs = :dayStartEpochMs LIMIT 1")
    suspend fun latestFor(dayStartEpochMs: Long): ReflectionEntity?
}
