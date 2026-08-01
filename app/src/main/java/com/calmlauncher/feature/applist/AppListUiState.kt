package com.calmlauncher.feature.applist

import com.calmlauncher.domain.model.AppDisplayMode
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.domain.model.UiRestrictionState

/**
 * Immutable snapshot the App List (full app drawer) renders. [apps] is the complete,
 * alphabetical set of launchable apps after [com.calmlauncher.domain.usecase.FilterAppsUseCase]
 * has applied behavioural visibility (hide-social, environment). [displayMode] selects
 * text-only (default) vs. icon rendering; [batteryText]/[signalText] feed the status bar and
 * [restriction] drives grayscale enforcement.
 */
data class AppListUiState(
    val apps: List<AppEntry> = emptyList(),
    val displayMode: AppDisplayMode = AppDisplayMode.TEXT,
    val batteryText: String = "",
    val signalText: String = "",
    val environmentMode: EnvironmentMode = EnvironmentMode.NONE,
    val restriction: UiRestrictionState = UiRestrictionState(),
)
