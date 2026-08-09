package com.calmlauncher.domain.repository

import com.calmlauncher.domain.model.Routine
import com.calmlauncher.domain.model.RoutineCompletion
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>
    fun observeCompletions(): Flow<List<RoutineCompletion>>
    suspend fun getRoutine(id: Long): Routine?
    suspend fun saveRoutine(routine: Routine): Long
    suspend fun deleteRoutine(id: Long)
    suspend fun setTaskCompletion(taskId: Long, dayStartEpochMs: Long, completion: RoutineCompletion?)
    suspend fun refreshRollover(nowEpochMs: Long)
}