package com.calmlauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.calmlauncher.data.db.entity.RoutineCompletionEntity
import com.calmlauncher.data.db.entity.RoutineEntity
import com.calmlauncher.data.db.entity.RoutineTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY createdAtEpochMs DESC, id DESC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine_tasks ORDER BY orderIndex ASC, id ASC")
    fun observeTasks(): Flow<List<RoutineTaskEntity>>

    @Query("SELECT * FROM routine_completions WHERE dayStartEpochMs >= :dayStartEpochMs")
    fun observeCompletionsFrom(dayStartEpochMs: Long): Flow<List<RoutineCompletionEntity>>

    @Query("SELECT * FROM routine_completions ORDER BY dayStartEpochMs DESC, timestampEpochMs DESC")
    fun observeCompletions(): Flow<List<RoutineCompletionEntity>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutine(id: Long): RoutineEntity?

    @Query("SELECT * FROM routines ORDER BY createdAtEpochMs DESC, id DESC")
    suspend fun getRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_tasks WHERE routineId = :routineId ORDER BY orderIndex ASC, id ASC")
    suspend fun getTasksForRoutine(routineId: Long): List<RoutineTaskEntity>

    @Query("SELECT * FROM routine_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTask(taskId: Long): RoutineTaskEntity?

    @Query("SELECT * FROM routine_tasks ORDER BY routineId ASC, orderIndex ASC, id ASC")
    suspend fun getTasks(): List<RoutineTaskEntity>

    @Query("SELECT * FROM routine_completions WHERE dayStartEpochMs = :dayStartEpochMs")
    suspend fun getCompletionsForDay(dayStartEpochMs: Long): List<RoutineCompletionEntity>

    @Insert
    suspend fun insertRoutine(entity: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(entity: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(entity: RoutineEntity)

    @Insert
    suspend fun insertTask(entity: RoutineTaskEntity): Long

    @Update
    suspend fun updateTask(entity: RoutineTaskEntity)

    @Delete
    suspend fun deleteTask(entity: RoutineTaskEntity)

    @Query("DELETE FROM routine_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM routine_completions WHERE taskId = :taskId")
    suspend fun deleteCompletionsForTask(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(entity: RoutineCompletionEntity)

    @Query("DELETE FROM routine_completions WHERE dayStartEpochMs < :dayStartEpochMs")
    suspend fun deleteCompletionsBefore(dayStartEpochMs: Long)

    @Query("DELETE FROM routine_completions WHERE taskId = :taskId AND dayStartEpochMs = :dayStartEpochMs")
    suspend fun clearCompletion(taskId: Long, dayStartEpochMs: Long)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Long)

    @Query("DELETE FROM routine_tasks WHERE routineId = :routineId")
    suspend fun deleteTasksForRoutine(routineId: Long)

    @Query("DELETE FROM routine_completions WHERE taskId IN (SELECT id FROM routine_tasks WHERE routineId = :routineId)")
    suspend fun deleteCompletionsForRoutine(routineId: Long)

    @Transaction
    suspend fun deleteRoutineCascade(routineId: Long) {
        deleteCompletionsForRoutine(routineId)
        deleteTasksForRoutine(routineId)
        deleteRoutineById(routineId)
    }
}