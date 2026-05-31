package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.calmlauncher.data.db.entity.AppUsageEntity
import com.calmlauncher.data.db.entity.DailyUsageEntity
import com.calmlauncher.data.db.entity.NotificationEventEntity
import com.calmlauncher.data.db.entity.SessionEntity
import com.calmlauncher.data.db.entity.UnlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {

    @Upsert
    suspend fun upsertDaily(entity: DailyUsageEntity)

    @Upsert
    suspend fun upsertApps(entities: List<AppUsageEntity>)

    @Insert
    suspend fun insertSessions(entities: List<SessionEntity>)

    @Insert
    suspend fun insertUnlock(entity: UnlockEntity)

    @Insert
    suspend fun insertNotification(entity: NotificationEventEntity)

    @Query("SELECT * FROM daily_usage WHERE dayStartEpochMs >= :startEpochMs AND dayStartEpochMs <= :endEpochMs ORDER BY dayStartEpochMs ASC")
    fun observeDailyRange(startEpochMs: Long, endEpochMs: Long): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE dayStartEpochMs >= :startEpochMs AND dayStartEpochMs <= :endEpochMs ORDER BY usageMinutes DESC, appName ASC")
    fun observeAppUsageRange(startEpochMs: Long, endEpochMs: Long): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM usage_session WHERE dayStartEpochMs >= :startEpochMs AND dayStartEpochMs <= :endEpochMs ORDER BY durationMinutes DESC, startTimeEpochMs ASC")
    fun observeSessionsRange(startEpochMs: Long, endEpochMs: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM unlock_event WHERE dayStartEpochMs >= :startEpochMs AND dayStartEpochMs <= :endEpochMs ORDER BY timestampEpochMs ASC")
    fun observeUnlocksRange(startEpochMs: Long, endEpochMs: Long): Flow<List<UnlockEntity>>

    @Query("SELECT * FROM notification_event WHERE dayStartEpochMs >= :startEpochMs AND dayStartEpochMs <= :endEpochMs ORDER BY timestampEpochMs ASC")
    fun observeNotificationsRange(startEpochMs: Long, endEpochMs: Long): Flow<List<NotificationEventEntity>>

    @Query("SELECT COUNT(*) FROM unlock_event WHERE dayStartEpochMs = :dayStartEpochMs")
    suspend fun unlockCountForDay(dayStartEpochMs: Long): Int

    @Query("SELECT COUNT(*) FROM notification_event WHERE dayStartEpochMs = :dayStartEpochMs")
    suspend fun notificationCountForDay(dayStartEpochMs: Long): Int

    @Query("DELETE FROM daily_usage WHERE dayStartEpochMs < :beforeEpochMs")
    suspend fun deleteDailyBefore(beforeEpochMs: Long)

    @Query("DELETE FROM app_usage WHERE dayStartEpochMs < :beforeEpochMs")
    suspend fun deleteAppsBefore(beforeEpochMs: Long)

    @Query("DELETE FROM usage_session WHERE dayStartEpochMs < :beforeEpochMs")
    suspend fun deleteSessionsBefore(beforeEpochMs: Long)

    @Query("DELETE FROM unlock_event WHERE dayStartEpochMs < :beforeEpochMs")
    suspend fun deleteUnlocksBefore(beforeEpochMs: Long)

    @Query("DELETE FROM notification_event WHERE dayStartEpochMs < :beforeEpochMs")
    suspend fun deleteNotificationsBefore(beforeEpochMs: Long)

    @Query("DELETE FROM daily_usage")
    suspend fun deleteAllDaily()

    @Query("DELETE FROM app_usage")
    suspend fun deleteAllApps()

    @Query("DELETE FROM usage_session")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM unlock_event")
    suspend fun deleteAllUnlocks()

    @Query("DELETE FROM notification_event")
    suspend fun deleteAllNotifications()
}
