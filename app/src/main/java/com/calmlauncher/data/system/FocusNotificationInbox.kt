package com.calmlauncher.data.system

import com.calmlauncher.domain.model.FocusNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Live notification previews used by the active Focus session. */
@Singleton
class FocusNotificationInbox @Inject constructor() {
    private val _notifications = MutableStateFlow<List<FocusNotification>>(emptyList())
    private val _silencedCount = MutableStateFlow(0)

    val notifications: StateFlow<List<FocusNotification>> = _notifications.asStateFlow()
    val silencedCount: StateFlow<Int> = _silencedCount.asStateFlow()

    fun show(notification: FocusNotification) {
        _notifications.update { current ->
            (listOf(notification) + current.filterNot { it.key == notification.key }).take(MAX_VISIBLE)
        }
    }

    fun remove(key: String) {
        _notifications.update { current -> current.filterNot { it.key == key } }
    }

    fun recordSilenced() {
        _silencedCount.update { it + 1 }
    }

    private companion object {
        const val MAX_VISIBLE = 3
    }
}
