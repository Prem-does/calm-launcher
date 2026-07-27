package com.calmlauncher.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.Reminder
import com.calmlauncher.domain.model.RepeatRule
import com.calmlauncher.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && upcoming.isEmpty() && completed.isEmpty()
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = reminderRepository.observeAll()
        .map { reminders ->
            val now = System.currentTimeMillis()
            val (done, open) = reminders.partition { it.completed }
            RemindersUiState(
                overdue = open.filter { it.isOverdue(now) },
                upcoming = open.filterNot { it.isOverdue(now) },
                completed = done.sortedByDescending { it.completedAtEpochMs ?: it.createdAtEpochMs },
            )
        }
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
}
