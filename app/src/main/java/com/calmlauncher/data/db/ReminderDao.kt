package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.calmlauncher.data.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    /**
     * Open reminders first, ordered by how soon they're due. Undated to-dos sort last
     * (`dueAtEpochMs IS NULL` yields 1) so anything with a deadline stays at the top.
     */
    @Query(
        """
        SELECT * FROM reminders
        ORDER BY completed ASC,
                 dueAtEpochMs IS NULL ASC,
                 dueAtEpochMs ASC,
                 createdAtEpochMs DESC
        """,
    )
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): ReminderEntity?

    /** Everything still open with a due time — the set that needs alarms after a reboot. */
    @Query("SELECT * FROM reminders WHERE completed = 0 AND dueAtEpochMs IS NOT NULL")
    suspend fun getPendingScheduled(): List<ReminderEntity>

    @Insert
    suspend fun insert(entity: ReminderEntity): Long

    @Update
    suspend fun update(entity: ReminderEntity)

    @Delete
    suspend fun delete(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reminders WHERE completed = 1")
    suspend fun deleteCompleted()
}
