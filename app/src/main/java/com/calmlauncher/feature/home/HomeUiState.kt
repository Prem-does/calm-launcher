package com.calmlauncher.feature.home

import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.UiRestrictionState

/**
 * Immutable snapshot the Home screen renders. The clock/date/battery/signal strings are
 * pre-formatted in the ViewModel so the composable stays presentational. [favorites] are
 * the user's home shortcuts, [insight] is the top neutral Calm AI line (or null), and
 * [restriction] drives grayscale enforcement.
 */
data class HomeUiState(
    val time: String = "",
    val date: String = "",
    val screenTimeText: String = "0m today",
    val batteryText: String = "",
    val signalText: String = "",
    val favorites: List<AppEntry> = emptyList(),
    val insight: String? = null,
    val restriction: UiRestrictionState = UiRestrictionState(),
)
