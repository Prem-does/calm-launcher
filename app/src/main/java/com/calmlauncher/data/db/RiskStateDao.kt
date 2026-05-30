package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.calmlauncher.data.db.entity.RiskStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RiskStateDao {

    @Upsert
    suspend fun upsert(entity: RiskStateEntity)

    @Query("SELECT * FROM risk_state WHERE id = :id LIMIT 1")
    fun observe(id: Int = RiskStateEntity.SINGLETON_ID): Flow<RiskStateEntity?>

    @Query("SELECT * FROM risk_state WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = RiskStateEntity.SINGLETON_ID): RiskStateEntity?
}
