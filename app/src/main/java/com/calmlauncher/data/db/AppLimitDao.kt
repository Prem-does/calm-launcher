package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.calmlauncher.data.db.entity.AppLimitEventEntity
import com.calmlauncher.data.db.entity.AppLimitGroupAssignmentEntity
import com.calmlauncher.data.db.entity.AppLimitRuleEntity
import com.calmlauncher.data.db.entity.AppLimitUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limit_rules ORDER BY enabled DESC, updatedAtEpochMs DESC")
    fun observeRules(): Flow<List<AppLimitRuleEntity>>

    @Query("SELECT * FROM app_limit_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRule(packageName: String): AppLimitRuleEntity?

    @Query("SELECT * FROM app_limit_rules ORDER BY enabled DESC, updatedAtEpochMs DESC")
    suspend fun getAllRules(): List<AppLimitRuleEntity>
    @Upsert
    suspend fun upsertRule(entity: AppLimitRuleEntity)

    @Query("DELETE FROM app_limit_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("SELECT * FROM app_limit_group_assignments ORDER BY updatedAtEpochMs DESC")
    fun observeGroupAssignments(): Flow<List<AppLimitGroupAssignmentEntity>>

    @Query("DELETE FROM app_limit_group_assignments WHERE groupId = :groupId")
    suspend fun deleteGroupAssignments(groupId: String)

    @Query("DELETE FROM app_limit_group_assignments WHERE packageName IN (:packageNames)")
    suspend fun deleteAssignmentsForPackages(packageNames: List<String>)

    @Upsert
    suspend fun upsertGroupAssignments(entities: List<AppLimitGroupAssignmentEntity>)

    @Query("SELECT * FROM app_limit_usage WHERE dayStartEpochMs = :dayStartEpochMs")
    fun observeUsage(dayStartEpochMs: Long): Flow<List<AppLimitUsageEntity>>

    @Upsert
    suspend fun upsertUsage(entity: AppLimitUsageEntity)

    @Query("DELETE FROM app_limit_usage WHERE dayStartEpochMs < :dayStartEpochMs")
    suspend fun deleteUsageBefore(dayStartEpochMs: Long)

    @Query("SELECT * FROM app_limit_events WHERE dayStartEpochMs = :dayStartEpochMs ORDER BY timestampEpochMs DESC")
    fun observeEvents(dayStartEpochMs: Long): Flow<List<AppLimitEventEntity>>

    @Upsert
    suspend fun upsertEvent(entity: AppLimitEventEntity)
}
