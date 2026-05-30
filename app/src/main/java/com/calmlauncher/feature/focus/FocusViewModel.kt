package com.calmlauncher.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.core.util.Quotes
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
    val quote: String = "",
    val grayscale: Boolean = false,
    val grayscaleAmount: Float = 0f,
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
) : ViewModel() {

    // Deterministic per-day quote: stable within a session, rotates across days.
    private val quote: String = Quotes.focusForDay(LocalDate.now().dayOfYear)

    val uiState: StateFlow<FocusUiState> = combine(
        settingsRepository.settings,
        observeRestriction(),
    ) { settings, restriction ->
        FocusUiState(
            active = settings.focusActive,
            quote = quote,
            grayscale = restriction.grayscale,
            grayscaleAmount = restriction.grayscaleAmount,
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
}
