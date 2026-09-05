package com.calmlauncher.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.Reminder
import com.calmlauncher.domain.model.Routine
import com.calmlauncher.domain.model.RoutineCompletion
import com.calmlauncher.domain.model.RepeatRule
import com.calmlauncher.domain.repository.ReminderRepository
import com.calmlauncher.domain.repository.RoutineRepository
import com.calmlauncher.feature.reminders.routines.RoutineDashboardUiState
import com.calmlauncher.feature.reminders.routines.buildRoutineDashboard
import com.calmlauncher.domain.model.dayStartEpochMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reminders split the way Samsung Reminder shows them: anything already due sits at the top,
 * then upcoming, then a collapsible done list.
 */
data class RemindersUiState(
    val overdue: List<Reminder> = emptyList(),
    val upcoming: List<Reminder> = emptyList(),
    val completed: List<Reminder> = emptyList(),
    val routineDashboard: RoutineDashboardUiState = RoutineDashboardUiState(),
) {
    val isEmpty: Boolean
        get() = overdue.isEmpty() && upcoming.isEmpty() && completed.isEmpty() && !routineDashboard.hasRoutines
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val routineRepository: RoutineRepository,
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = combine(
        reminderRepository.observeAll(),
        routineRepository.observeRoutines(),
        routineRepository.observeCompletions(),
    ) { reminders, routines, completions ->
        val now = System.currentTimeMillis()
        val (done, open) = reminders.partition { it.completed }
        RemindersUiState(
            overdue = open.filter { it.isOverdue(now) },
            upcoming = open.filterNot { it.isOverdue(now) },
            completed = done.sortedByDescending { it.completedAtEpochMs ?: it.createdAtEpochMs },
            routineDashboard = buildRoutineDashboard(routines, completions, now),
        )
    }
        // The combine block runs wherever the flow is collected, which is viewModelScope — the
        // main thread. Grouping every completion row and walking the streak window is real work,
        // so it is pushed onto a background dispatcher rather than trusted to stay cheap.
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RemindersUiState(),
        )

    fun save(
        id: Long,
        title: String,
        note: String,
        dueAtEpochMs: Long?,
        repeatRule: RepeatRule,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = if (id == 0L) null else reminderRepository.get(id)
            reminderRepository.save(
                Reminder(
                    id = id,
                    title = trimmed,
                    note = note.trim(),
                    dueAtEpochMs = dueAtEpochMs,
                    // A repeat only makes sense alongside a due time.
                    repeatRule = if (dueAtEpochMs == null) RepeatRule.NONE else repeatRule,
                    completed = false,
                    completedAtEpochMs = null,
                    createdAtEpochMs = existing?.createdAtEpochMs ?: System.currentTimeMillis(),
                ),
            )
        }
    }

    fun setCompleted(reminder: Reminder, completed: Boolean) {
        viewModelScope.launch { reminderRepository.setCompleted(reminder.id, completed) }
    }

    fun snooze(reminder: Reminder, minutes: Int) {
        viewModelScope.launch { reminderRepository.snooze(reminder.id, minutes) }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch { reminderRepository.delete(reminder.id) }
    }

    fun clearCompleted() {
        viewModelScope.launch { reminderRepository.deleteCompleted() }
    }

    fun saveRoutine(routine: Routine) {
        val title = routine.title.trim()
        val normalized = routine.copy(
            title = title,
            tasks = routine.tasks.map { task ->
                task.copy(title = task.title.trim(), unit = task.unit.trim())
            },
        )
        // Keep malformed drafts out even if this method is called from somewhere other than the
        // editor. A routine with blank tasks cannot be meaningfully completed or scheduled.
        if (
            normalized.title.isEmpty() || normalized.activeDaysMask == 0 || normalized.tasks.isEmpty() ||
            normalized.tasks.any { task ->
                task.title.isEmpty() ||
                    (task.taskType == com.calmlauncher.domain.model.RoutineTaskType.METRIC &&
                        (task.targetValue == null || task.targetValue <= 0))
            }
        ) return
        viewModelScope.launch {
            routineRepository.saveRoutine(normalized)
        }
    }

    fun deleteRoutine(routineId: Long) {
        if (routineId == 0L) return
        viewModelScope.launch { routineRepository.deleteRoutine(routineId) }
    }

    fun setRoutineEnabled(routine: Routine, enabled: Boolean) {
        viewModelScope.launch {
            routineRepository.saveRoutine(routine.copy(enabled = enabled))
        }
    }

    fun setRoutineCheckbox(taskId: Long, checked: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayStart = dayStartEpochMs(now)
            routineRepository.setTaskCompletion(
                taskId = taskId,
                dayStartEpochMs = dayStart,
                completion = if (checked) {
                    RoutineCompletion(
                        taskId = taskId,
                        dayStartEpochMs = dayStart,
                        completed = true,
                        actualValue = null,
                        timestampEpochMs = now,
                    )
                } else {
                    null
                },
            )
        }
    }

    fun setRoutineMetricValue(taskId: Long, value: Int?, completed: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayStart = dayStartEpochMs(now)
            routineRepository.setTaskCompletion(
                taskId = taskId,
                dayStartEpochMs = dayStart,
                completion = if (value == null) {
                    null
                } else {
                    RoutineCompletion(
                        taskId = taskId,
                        dayStartEpochMs = dayStart,
                        completed = completed,
                        actualValue = value,
                        timestampEpochMs = now,
                    )
                },
            )
        }
    }
}
