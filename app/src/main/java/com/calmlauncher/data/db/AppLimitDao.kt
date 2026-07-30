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

    @Query("SELECT packageName FROM app_limit_group_assignments WHERE groupId = :groupId")
    suspend fun getPackagesInGroup(groupId: String): List<String>

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

    /**
     * Extensions recorded in the event log for one app today.
     *
     * The rule's own ledger is the primary record, but the event log outlives the rule: deleting a
     * limit and adding it back would otherwise hand out a fresh allowance. Taking the higher of
     * the two closes that door, and the SQL aggregate costs a fraction of loading every event and
     * counting in Kotlin, which is what this replaced.
     */
    @Query(
        """
        SELECT COUNT(*) FROM app_limit_events
        WHERE dayStartEpochMs = :dayStartEpochMs
          AND packageName = :packageName
          AND eventType = 'OVERRIDE'
        """,
    )
    suspend fun countOverridesOn(dayStartEpochMs: Long, packageName: String): Int

    /** Extra minutes recorded in the event log for one app today. See [countOverridesOn]. */
    @Query(
        """
        SELECT COALESCE(SUM(overrideMinutes), 0) FROM app_limit_events
        WHERE dayStartEpochMs = :dayStartEpochMs
          AND packageName = :packageName
          AND eventType = 'OVERRIDE'
        """,
    )
    suspend fun sumOverrideMinutesOn(dayStartEpochMs: Long, packageName: String): Int

    /**
     * Spend [minutes] of extension budget and push the block out to [overrideUntilEpochMs], but
     * only if the ledger still has room for it.
     *
     * The `WHERE` clause is the exploit fix. Checking the budget in Kotlin and then writing the
     * grant leaves a window in which two taps — or the block overlay and the App Limits screen
     * racing each other — both read "one extension left" and both spend it. Here the check and
     * the spend are one statement, so the second attempt updates zero rows and is refused.
     *
     * `overrideDayStartEpochMs != :dayStartEpochMs` is the day-rollover case: the ledger describes
     * a previous day, so the counters are overwritten rather than added to.
     */
    @Query(
        """
        UPDATE app_limit_rules
        SET overrideUntilEpochMs = :overrideUntilEpochMs,
            updatedAtEpochMs = :nowEpochMs,
            overrideDayStartEpochMs = :dayStartEpochMs,
            overridesUsedToday = CASE
                WHEN overrideDayStartEpochMs = :dayStartEpochMs THEN overridesUsedToday + 1
                ELSE 1 END,
            overrideMinutesUsedToday = CASE
                WHEN overrideDayStartEpochMs = :dayStartEpochMs
                    THEN overrideMinutesUsedToday + :minutes
                ELSE :minutes END,
            lastNotifiedStage = 'NONE',
            lastNotifiedDayStartEpochMs = :dayStartEpochMs
        WHERE packageName = :packageName
          AND enabled = 1
          AND (
            overrideDayStartEpochMs != :dayStartEpochMs
            OR (overridesUsedToday < :maxExtensions
                AND overrideMinutesUsedToday + :minutes <= :maxExtraMinutes)
          )
        """,
    )
    suspend fun spendOverrideBudget(
        packageName: String,
        minutes: Int,
        overrideUntilEpochMs: Long,
        nowEpochMs: Long,
        dayStartEpochMs: Long,
        maxExtensions: Int,
        maxExtraMinutes: Int,
    ): Int
}
