package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.model.ScreenTimeRecord
import com.calmlauncher.domain.model.UiRestrictionState
import com.calmlauncher.domain.repository.ScreenTimeRepository
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.domain.usecase.ObserveRestrictionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Human-readable label for an [EnvironmentMode]. Kept local to the settings feature so the
 * domain enum stays free of presentation concerns. NONE→"None", DEEP_WORK→"Deep Work", etc.
 */
fun EnvironmentMode.displayName(): String = when (this) {
    EnvironmentMode.NONE -> "None"
    EnvironmentMode.WORK -> "Work"
    EnvironmentMode.STUDY -> "Study"
    EnvironmentMode.DEEP_WORK -> "Deep Work"
    EnvironmentMode.SLEEP -> "Sleep"
    EnvironmentMode.GYM -> "Gym"
    EnvironmentMode.OUTSIDE -> "Outside"
    EnvironmentMode.TRAVEL -> "Travel"
}

/**
 * Drives the Settings hub. Surfaces the live [LauncherSettings] snapshot, today's pre-formatted
 * screen-time string and the derived [UiRestrictionState] (so the surface can desaturate when a
 * mode enforces grayscale). All preference mutations funnel through [update], which composes a
 * copy over the [SettingsRepository] — matching the `update { it.copy(...) }` convention used by
 * every toggle on the screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    screenTimeRepository: ScreenTimeRepository,
    observeRestriction: ObserveRestrictionStateUseCase,
) : ViewModel() {

    val settings: StateFlow<LauncherSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LauncherSettings(),
    )

    /** Today's foreground total, pre-formatted as e.g. "1h 12m today" for the row value. */
    val screenTimeText: StateFlow<String> = screenTimeRepository.observeToday()
        .map { "${it.format()} today" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ScreenTimeRecord.empty(0L).format() + " today",
        )

    val restriction: StateFlow<UiRestrictionState> = observeRestriction().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UiRestrictionState(),
    )

    /** Compose an immutable copy over the persisted settings, e.g. `update { it.copy(...) }`. */
    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
