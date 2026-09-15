package com.calmlauncher.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.core.util.Quotes
import com.calmlauncher.core.util.TimeFormatter
import com.calmlauncher.data.system.ClockTicker
import com.calmlauncher.data.system.FocusNotificationInbox
import com.calmlauncher.domain.model.FocusNotification
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.domain.usecase.ObserveRestrictionStateUseCase
import com.calmlauncher.domain.usecase.ToggleFocusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** The default focus session length used when the user has never chosen one. */
private const val DefaultFocusMinutes = 25

/**
 * Presentational state for the Focus screen: whether a session is live, the day's stable
 * quote, and the grayscale posture lifted from the restriction state so the canvas can
 * desaturate in sync with the rest of the launcher.
 */
data class FocusUiState(
    val active: Boolean = false,
    val currentTime: String = "--:--",
    val quote: String = "",
    val grayscale: Boolean = false,
    val grayscaleAmount: Float = 0f,
    val einkTexture: Boolean = false,
    val remainingText: String = "--:--",
    val elapsedText: String = "0m",
    val progressFraction: Float = 0f,
    val durationMinutes: Int = DefaultFocusMinutes,
    val durationSeconds: Int = DefaultFocusMinutes * 60,
    val intention: String = "",
    val notifications: List<FocusNotification> = emptyList(),
    val silencedNotificationCount: Int = 0,
)

/**
 * Drives the Focus screen. Collapses the live settings (focus flag) and restriction
 * (grayscale) streams into a single [FocusUiState], picking a deterministic quote for the
 * current day so the message stays stable for the whole session. Starting and stopping a
 * session is delegated to [ToggleFocusUseCase], which persists the flags the ModeEngine
 * reads to hard-block quick-exit apps.
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val toggleFocus: ToggleFocusUseCase,
    observeRestriction: ObserveRestrictionStateUseCase,
    clockTicker: ClockTicker,
    focusNotificationInbox: FocusNotificationInbox,
) : ViewModel() {

    // Deterministic per-day quote: stable within a session, rotates across days.
    private val quote: String = Quotes.focusForDay(LocalDate.now().dayOfYear)

    val uiState: StateFlow<FocusUiState> = combine(
        settingsRepository.settings,
        observeRestriction(),
        clockTicker.time,
        focusNotificationInbox.notifications,
        focusNotificationInbox.silencedCount,
    ) { settings, restriction, now, notifications, silencedCount ->
        val totalDurationSeconds = settings.focusDurationSeconds.takeIf { it > 0 }
            ?: settings.focusDurationMinutes.coerceAtLeast(1) * 60
        val remainingMs = if (settings.focusActive) {
            val endAt = settings.focusStartedAtEpochMs +
                totalDurationSeconds * SECOND_MS
            (endAt - now).coerceAtLeast(0L)
        } else {
            totalDurationSeconds * SECOND_MS
        }
        val durationMs = totalDurationSeconds * SECOND_MS
        val elapsedMs = if (settings.focusActive) (durationMs - remainingMs).coerceIn(0L, durationMs) else 0L
        FocusUiState(
            active = settings.focusActive,
            currentTime = TimeFormatter.formatHomeTime(now),
            quote = quote,
            grayscale = restriction.grayscale,
            grayscaleAmount = restriction.grayscaleAmount,
            einkTexture = settings.einkSimulationEnabled,
            remainingText = formatRemaining(remainingMs),
            elapsedText = formatRemaining(elapsedMs),
            progressFraction = elapsedMs.toFloat() / durationMs.toFloat(),
            durationMinutes = settings.focusDurationMinutes.coerceIn(1, 180),
            durationSeconds = totalDurationSeconds,
            intention = settings.focusIntention,
            notifications = notifications,
            silencedNotificationCount = silencedCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FocusUiState(quote = quote),
    )

    /** Begin a session if one isn't already running, honoring the saved duration default. */
    fun startIfNeeded() {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            if (!settings.focusActive) {
                val duration = settings.focusDurationMinutes.takeIf { it > 0 } ?: DefaultFocusMinutes
                toggleFocus.start(duration)
            }
        }
    }

    /** End the current focus session. */
    fun endFocus() {
        viewModelScope.launch { toggleFocus.stop() }
    }

    /** Persist the session's one meaningful outcome, clipped to stay readable on the canvas. */
    fun saveIntention(value: String) {
        val intention = value.trim().take(MAX_INTENTION_LENGTH)
        viewModelScope.launch {
            settingsRepository.update { it.copy(focusIntention = intention) }
        }
    }

    fun setFocusDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(
                    focusDurationMinutes = minutes.coerceIn(1, MAX_DURATION_MINUTES),
                    focusDurationSeconds = minutes.coerceIn(1, MAX_DURATION_MINUTES) * 60,
                )
            }
        }
    }

    /** Apply an exact timer value and restart the session from the new duration. */
    fun applyFocusDuration(seconds: Int) {
        viewModelScope.launch {
            val safeSeconds = seconds.coerceIn(1, MAX_DURATION_MINUTES * 60)
            settingsRepository.update {
                it.copy(
                    focusActive = true,
                    focusStartedAtEpochMs = System.currentTimeMillis(),
                    focusDurationMinutes = ((safeSeconds + 59) / 60).coerceIn(1, MAX_DURATION_MINUTES),
                    focusDurationSeconds = safeSeconds,
                )
            }
        }
    }

    /** Adjust the live remaining time while preserving the elapsed part of the session. */
    fun adjustRemainingMinutes(delta: Int) {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val now = System.currentTimeMillis()
            val durationMs = settings.focusDurationMinutes.coerceAtLeast(1) * MINUTE_MS
            val startedAt = settings.focusStartedAtEpochMs.takeIf { it > 0L } ?: now
            val elapsedMs = (now - startedAt).coerceIn(0L, durationMs)
            val remainingMs = (durationMs - elapsedMs).coerceAtLeast(MINUTE_MS)
            val adjustedRemainingMs = (remainingMs + delta * MINUTE_MS)
                .coerceIn(MINUTE_MS, MAX_DURATION_MINUTES * MINUTE_MS)
            val adjustedDurationMinutes = ((elapsedMs + adjustedRemainingMs + MINUTE_MS - 1) / MINUTE_MS)
                .toInt()
                .coerceIn(1, MAX_DURATION_MINUTES)
            settingsRepository.update {
                it.copy(
                    focusStartedAtEpochMs = now - elapsedMs,
                    focusDurationMinutes = adjustedDurationMinutes,
                )
            }
        }
    }

    private fun formatRemaining(remainingMs: Long): String {
        val totalSeconds = ((remainingMs + SECOND_MS - 1) / SECOND_MS).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val SECOND_MS = 1_000L
        const val MAX_INTENTION_LENGTH = 120
        const val MAX_DURATION_MINUTES = 720
    }
}
