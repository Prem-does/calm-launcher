package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.Insight
import com.calmlauncher.domain.model.ScreenTimeRecord
import com.calmlauncher.domain.repository.ScreenTimeRepository
import com.calmlauncher.domain.usecase.BuildInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** A single app's foreground usage for the day, pre-formatted for display. */
data class AppUsage(
    val packageName: String,
    val label: String,
    val duration: String,
)

/** Everything the Screen Time screen renders: today's total, the top apps and calm insights. */
data class ScreenTimeUiState(
    val total: String = "0m",
    val topApps: List<AppUsage> = emptyList(),
    val insights: List<Insight> = emptyList(),
)

/**
 * Drives Screen Time. Combines today's [ScreenTimeRecord] with the Calm AI [BuildInsightsUseCase]
 * stream into a single [ScreenTimeUiState] — pre-formatting the total and ranking the busiest apps
 * so the screen stays presentational.
 */
@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    screenTimeRepository: ScreenTimeRepository,
    buildInsights: BuildInsightsUseCase,
) : ViewModel() {

    val uiState: StateFlow<ScreenTimeUiState> = combine(
        screenTimeRepository.observeToday(),
        buildInsights(),
    ) { record, insights ->
        ScreenTimeUiState(
            total = record.format(),
            topApps = record.toTopApps(),
            insights = insights,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ScreenTimeUiState(),
    )

    /** Rank apps by foreground time (desc), keep the busiest few, format each duration. */
    private fun ScreenTimeRecord.toTopApps(): List<AppUsage> =
        perApp.entries
            .sortedByDescending { it.value }
            .take(MAX_TOP_APPS)
            .map { (packageName, millis) ->
                AppUsage(
                    packageName = packageName,
                    label = friendlyName(packageName),
                    duration = formatDuration(millis),
                )
            }

    /** "com.instagram.android" -> "Instagram"; falls back to the raw id. */
    private fun friendlyName(packageName: String): String {
        val candidate = packageName
            .substringBefore("/")
            .split('.')
            .lastOrNull { it.isNotBlank() && it != "android" && it != "app" }
            ?: packageName
        return candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /** "1h 12m" / "12m" / "<1m" style, mirroring [ScreenTimeRecord.format]. */
    private fun formatDuration(millis: Long): String {
        val minutes = millis / 60_000L
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m"
            else -> "<1m"
        }
    }

    private companion object {
        const val MAX_TOP_APPS = 6
    }
}
