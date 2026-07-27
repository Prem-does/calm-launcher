package com.calmlauncher.feature.home

import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.domain.model.UiRestrictionState

/**
 * Immutable snapshot the Home screen renders. The clock/date/battery/signal strings are
 * pre-formatted in the ViewModel so the composable stays presentational. [favorites] are
 * the user's home shortcuts, [insight] is the top neutral Calm AI line (or null),
 * [environmentMode] is the active context preset, and [restriction] drives grayscale
 * enforcement.
 */
data class HomeUiState(
    val time: String = "",
    val date: String = "",
    val screenTimeText: String = "0m today",
    val batteryText: String = "",
    val signalText: String = "",
    val environmentMode: EnvironmentMode = EnvironmentMode.NONE,
    val favorites: List<AppEntry> = emptyList(),
    val insight: String? = null,
    val restriction: UiRestrictionState = UiRestrictionState(),
) {
    /** Home only shows the environment chip when a preset is actually active. */
    val showEnvironment: Boolean get() = environmentMode != EnvironmentMode.NONE
}
