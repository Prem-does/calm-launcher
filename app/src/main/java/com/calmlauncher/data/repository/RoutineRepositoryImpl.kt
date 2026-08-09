package com.calmlauncher.data.repository

import com.calmlauncher.data.db.RoutineDao
import com.calmlauncher.data.db.entity.RoutineCompletionEntity
import com.calmlauncher.data.db.entity.RoutineEntity
import com.calmlauncher.data.db.entity.RoutineTaskEntity
import com.calmlauncher.di.IoDispatcher
import com.calmlauncher.domain.model.Routine
import com.calmlauncher.domain.model.RoutineCompletion
import com.calmlauncher.domain.model.RoutineTask
import com.calmlauncher.domain.model.RoutineTaskType
import com.calmlauncher.domain.model.dayStartEpochMs
import com.calmlauncher.domain.repository.RoutineRepository
import com.calmlauncher.notification.RoutineNotificationManager
import com.calmlauncher.notification.RoutineScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val scheduler: RoutineScheduler,
    private val notifications: RoutineNotificationManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> = combine(
        routineDao.observeRoutines(),
        routineDao.observeTasks(),
    ) { routines, tasks ->
        val tasksByRoutine = tasks.groupBy { it.routineId }
        routines.map { entity ->
            entity.toDomain(tasks = tasksByRoutine[entity.id].orEmpty().map { it.toDomain() })
        }
    }.flowOn(dispatcher)

    override fun observeCompletions(): Flow<List<RoutineCompletion>> =
        routineDao.observeCompletions().map { entities -> entities.map { it.toDomain() } }.flowOn(dispatcher)

    override suspend fun getRoutine(id: Long): Routine? = withContext(dispatcher) {
        val routine = routineDao.getRoutine(id) ?: return@withContext null
        val tasks = routineDao.getTasksForRoutine(id).map { it.toDomain() }
        routine.toDomain(tasks = tasks)
    }

    override suspend fun saveRoutine(routine: Routine): Long = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        if (routine.id == 0L) {
            val routineId = routineDao.insertRoutine(
                routine.copy(createdAtEpochMs = now).toEntity(),
            )
            routine.tasks.mapIndexed { index, task ->
                routineDao.insertTask(
                    task.copy(
                        routineId = routineId,
                        orderIndex = index,
                    ).toEntity(),
                )
            }
            syncTaskAlarms(routineId)
            return@withContext routineId
        }

        routineDao.updateRoutine(routine.toEntity())
        val existingTasks = routineDao.getTasksForRoutine(routine.id)
        val existingById = existingTasks.associateBy { it.id }
        val incomingIds = routine.tasks.filter { it.id != 0L }.map { it.id }.toSet()

        existingTasks.filterNot { it.id in incomingIds }.forEach { removed ->
            routineDao.deleteCompletionsForTask(removed.id)
            routineDao.deleteTaskById(removed.id)
        }

        routine.tasks.mapIndexed { index, task ->
            val normalized = task.copy(routineId = routine.id, orderIndex = index)
            if (normalized.id == 0L || existingById[normalized.id] == null) {
                routineDao.insertTask(normalized.toEntity())
            } else {
                routineDao.updateTask(normalized.toEntity())
            }
        }

        syncTaskAlarms(routine.id)

        routine.id
    }

    override suspend fun deleteRoutine(id: Long) = withContext(dispatcher) {
        routineDao.getTasksForRoutine(id).forEach { task ->
            scheduler.cancel(task.id)
            notifications.cancel(task.id)
        }
        routineDao.deleteRoutineCascade(id)
    }

    override suspend fun setTaskCompletion(
        taskId: Long,
        dayStartEpochMs: Long,
        completion: RoutineCompletion?,
    ) = withContext(dispatcher) {
        if (completion == null) {
            routineDao.clearCompletion(taskId, dayStartEpochMs)
        } else {
            routineDao.upsertCompletion(completion.toEntity())
        }
        val task = routineDao.getTask(taskId) ?: return@withContext
        val routine = routineDao.getRoutine(task.routineId) ?: return@withContext
        val domainTask = task.toDomain()
        notifications.cancel(taskId)
        if (domainTask.reminderMinuteOfDay != null) {
            scheduler.schedule(routine.toDomain(tasks = listOf(domainTask)), domainTask)
        }
    }

    override suspend fun refreshRollover(nowEpochMs: Long) = withContext(dispatcher) {
        // Completion rows are day-scoped already. The rollover job just trims old rows so the
        // table stays bounded and the next day naturally starts with a clean slate.
        val cutoff = nowEpochMs - 180L * 86_400_000L
        routineDao.deleteCompletionsBefore(dayStartEpochMs(cutoff))
        scheduler.rescheduleAll(
            routineDao.getRoutines().map { routine ->
                routine.toDomain(routineDao.getTasksForRoutine(routine.id).map { it.toDomain() })
            },
        )
    }

    private suspend fun syncTaskAlarms(routineId: Long) {
        val routine = routineDao.getRoutine(routineId) ?: return
        val tasks = routineDao.getTasksForRoutine(routineId).map { it.toDomain() }
        tasks.forEach { task ->
            notifications.cancel(task.id)
        }
        scheduler.rescheduleAll(listOf(routine.toDomain(tasks = tasks)))
    }

    private fun RoutineEntity.toDomain(tasks: List<RoutineTask>): Routine =
        Routine(
            id = id,
            title = title,
            activeDaysMask = activeDaysMask,
            createdAtEpochMs = createdAtEpochMs,
            tasks = tasks,
        )

    private fun RoutineTaskEntity.toDomain() = RoutineTask(
        id = id,
        routineId = routineId,
        title = title,
        taskType = runCatching { RoutineTaskType.valueOf(taskType) }.getOrDefault(RoutineTaskType.CHECKBOX),
        targetValue = targetValue,
        unit = unit,
        reminderMinuteOfDay = reminderMinuteOfDay,
        orderIndex = orderIndex,
    )

    private fun RoutineTask.toEntity() = RoutineTaskEntity(
        id = id,
        routineId = routineId,
        title = title,
        taskType = taskType.name,
        targetValue = targetValue,
        unit = unit,
        reminderMinuteOfDay = reminderMinuteOfDay,
        orderIndex = orderIndex,
    )

    private fun RoutineCompletionEntity.toDomain() = RoutineCompletion(
        taskId = taskId,
        dayStartEpochMs = dayStartEpochMs,
        completed = completed,
        actualValue = actualValue,
        timestampEpochMs = timestampEpochMs,
    )

    private fun RoutineCompletion.toEntity() = RoutineCompletionEntity(
        taskId = taskId,
        dayStartEpochMs = dayStartEpochMs,
        completed = completed,
        actualValue = actualValue,
        timestampEpochMs = timestampEpochMs,
    )

    private fun Routine.toEntity() = RoutineEntity(
        id = id,
        title = title,
        activeDaysMask = activeDaysMask,
        createdAtEpochMs = createdAtEpochMs,
    )
}