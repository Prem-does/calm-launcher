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

    /**
     * Atomically claim the right to announce the occurrence due at [occurrenceEpochMs],
     * returning the number of rows changed: 1 when this caller won the claim, 0 when the
     * occurrence had already been claimed (or the reminder is gone or completed).
     *
     * This single statement is what makes reminder delivery exactly-once. Several independent
     * callers legitimately try to fire the same reminder — the exact alarm, a redundant alarm
     * delivery after a doze exit, the boot re-arm sweep, the `MY_PACKAGE_REPLACED` sweep — and
     * none of them can see what the others have done. Because the compare and the write happen
     * inside one SQL UPDATE, exactly one of them observes a non-zero result and the rest go
     * quiet. Checking-then-writing in Kotlin would leave a window for two notifications.
     *
     * The `completed = 0` guard also closes the race where a reminder is ticked off in the gap
     * between the alarm being set and it going off.
     */
    @Query(
        """
        UPDATE reminders
        SET lastFiredOccurrenceEpochMs = :occurrenceEpochMs
        WHERE id = :id
          AND completed = 0
          AND dueAtEpochMs = :occurrenceEpochMs
          AND (lastFiredOccurrenceEpochMs IS NULL
               OR lastFiredOccurrenceEpochMs != :occurrenceEpochMs)
        """,
    )
    suspend fun claimOccurrence(id: Long, occurrenceEpochMs: Long): Int

    /**
     * Forget the claim, so the reminder's current occurrence may be announced again. Used when
     * the user edits a reminder's due time — a rescheduled reminder is a new obligation, not a
     * repeat of one already delivered.
     */
    @Query("UPDATE reminders SET lastFiredOccurrenceEpochMs = NULL WHERE id = :id")
    suspend fun clearOccurrenceClaim(id: Long)

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
