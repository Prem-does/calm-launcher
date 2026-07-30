package com.calmlauncher.feature.reminderalert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.Reminder
import com.calmlauncher.domain.repository.ReminderRepository
import com.calmlauncher.notification.ReminderNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The snooze durations offered on the overlay, in minutes. */
val SnoozeChoices = listOf(5, 10, 15, 30, 60)

/**
 * What the overlay is currently showing.
 *
 * [Dismissed] is a terminal state the Activity watches so it can finish itself. Having the
 * ViewModel own it — rather than the Activity calling `finish()` inline — means the write to the
 * database always completes before the screen goes away, so a "Finished" tap can't be lost by
 * the process being torn down a frame later.
 */
sealed interface ReminderAlertUiState {
    data object Loading : ReminderAlertUiState
    data class Showing(
        val reminder: Reminder,
        val snoozePickerOpen: Boolean = false,
        val busy: Boolean = false,
    ) : ReminderAlertUiState

    data object Dismissed : ReminderAlertUiState
}

@HiltViewModel
class ReminderAlertViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val notifications: ReminderNotificationManager,
) : ViewModel() {

    private val _state = MutableStateFlow<ReminderAlertUiState>(ReminderAlertUiState.Loading)
    val state: StateFlow<ReminderAlertUiState> = _state.asStateFlow()

    private var reminderId: Long = -1L

    /**
     * Bind the overlay to a reminder. Safe to call again with the same id (configuration change,
     * a second full-screen intent for an alarm already showing) — the first bind wins and the
     * repeat is ignored, so rotating the device can't reset the snooze picker or double up work.
     */
    fun bind(id: Long) {
        if (reminderId == id && _state.value !is ReminderAlertUiState.Loading) return
        reminderId = id
        viewModelScope.launch {
            val reminder = reminderRepository.get(id)
            _state.value = if (reminder == null || reminder.completed) {
                // Dealt with elsewhere (notification action, or the reminders list) between the
                // alarm firing and this screen coming up. Nothing to interrupt anyone for.
                ReminderAlertUiState.Dismissed
            } else {
                ReminderAlertUiState.Showing(reminder)
            }
        }
    }

    fun openSnoozePicker() = updateShowing { it.copy(snoozePickerOpen = true) }

    fun closeSnoozePicker() = updateShowing { it.copy(snoozePickerOpen = false) }

    /**
     * Snooze by [minutes]. The repository moves the due time and clears the delivery claim in one
     * write, which both re-arms exactly one alarm and makes the new occurrence eligible to fire —
     * so a snooze can never leave two reminders queued for the same task.
     */
    fun snooze(minutes: Int) = act { id ->
        notifications.cancel(id)
        reminderRepository.snooze(id, minutes)
    }

    /**
     * Mark finished. For a repeating reminder the series was already advanced when it fired, so
     * this only retires the occurrence in front of the user; the repository decides whether that
     * means completing the reminder or leaving the series running.
     */
    fun finish() = act { id ->
        notifications.cancel(id)
        reminderRepository.setCompleted(id, true)
    }

    /**
     * Runs a terminal action exactly once, guarding against a double tap on a slow write. The
     * `busy` flag disables the buttons while the write is in flight rather than queuing a second
     * one behind it.
     */
    private fun act(block: suspend (Long) -> Unit) {
        val showing = _state.value as? ReminderAlertUiState.Showing ?: return
        if (showing.busy) return
        val id = reminderId
        if (id < 0) return
        _state.value = showing.copy(busy = true, snoozePickerOpen = false)
        viewModelScope.launch {
            runCatching { block(id) }
            _state.value = ReminderAlertUiState.Dismissed
        }
    }

    private fun updateShowing(transform: (ReminderAlertUiState.Showing) -> ReminderAlertUiState) {
        val showing = _state.value as? ReminderAlertUiState.Showing ?: return
        if (showing.busy) return
        _state.value = transform(showing)
    }
}
