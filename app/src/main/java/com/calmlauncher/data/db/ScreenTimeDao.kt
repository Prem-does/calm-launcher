package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calmlauncher.data.db.entity.ScreenTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenTimeDao {
    @Query("SELECT * FROM screen_time WHERE dayKey = :dayKey LIMIT 1")
    fun observeDay(dayKey: String): Flow<ScreenTimeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScreenTimeEntity)

    @Query("SELECT * FROM screen_time WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getDay(dayKey: String): ScreenTimeEntity?
}
