package com.calmlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.core.util.TimeFormatter
import com.calmlauncher.data.system.BatteryObserver
import com.calmlauncher.data.system.ClockTicker
import com.calmlauncher.data.system.ConnectivityObserver
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.domain.model.LaunchSource
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.ScreenTimeRepository
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.domain.usecase.BuildInsightsUseCase
import com.calmlauncher.domain.usecase.ObserveRestrictionStateUseCase
import com.calmlauncher.launcher.LaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Home screen. Collapses the live system/clock/app/insight/restriction streams
 * into a single [HomeUiState], pre-formatting the clock, date and battery strings so the UI
 * stays presentational. App opens are delegated to the central [LaunchCoordinator] so they
 * pass through the friction pipeline.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    clockTicker: ClockTicker,
    batteryObserver: BatteryObserver,
    connectivityObserver: ConnectivityObserver,
    settingsRepository: SettingsRepository,
    private val screenTimeRepository: ScreenTimeRepository,
    buildInsights: BuildInsightsUseCase,
    observeRestriction: ObserveRestrictionStateUseCase,
    private val appRepository: AppRepository,
    private val launchCoordinator: LaunchCoordinator,
) : ViewModel() {

    // Clock emits epoch millis → pre-format both the 12-hour time and the long date.
    private val clock = clockTicker.time.map { now ->
        ClockText(
            time = TimeFormatter.formatTime(now, use24h = false),
            date = TimeFormatter.formatDate(now),
        )
    }

    private val battery = batteryObserver.status.map { "${it.percent}%" }

    // Without Usage Access the reading is always "0m today", which reads as a real number
    // rather than a missing permission. Say what's actually wrong instead.
    private val screenTime = screenTimeRepository.observeToday().map { record ->
        if (screenTimeRepository.hasUsageAccess()) {
            "${record.format()} today"
        } else {
            "Screen time needs Usage Access"
        }
    }

    private val topInsight = buildInsights().map { it.firstOrNull()?.text }

    // combine() takes at most 5 flows directly; group the system/status streams into one
    // combine, then fold that together with the remaining flows in an outer combine.
    private val system = combine(
        clock,
        battery,
        connectivityObserver.signal,
        settingsRepository.settings.map { it.environmentMode },
    ) { clockText, batteryText, signalText, environment ->
        SystemShell(clockText, batteryText, signalText, environment)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        system,
        screenTime,
        appRepository.observeFavorites(),
        topInsight,
        observeRestriction(),
    ) { shell, screenTimeText, favorites, insight, restriction ->
        HomeUiState(
            time = shell.clock.time,
            date = shell.clock.date,
            screenTimeText = screenTimeText,
            batteryText = shell.battery,
            signalText = shell.signal,
            environmentMode = shell.environment,
            favorites = favorites,
            insight = insight,
            restriction = restriction,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(),
    )

    /** Open a favorite through the friction pipeline. */
    fun open(app: AppEntry) = launchCoordinator.request(
        AppLaunchRequest(
            packageName = app.packageName,
            label = app.label,
            category = app.category,
            source = LaunchSource.HOME,
        ),
    )

    /** Unpin a shortcut straight from Home (long-press). Reversible in Manage Apps. */
    fun unpin(app: AppEntry) {
        viewModelScope.launch { appRepository.setFavorite(app.packageName, false) }
    }

    private data class ClockText(val time: String, val date: String)

    private data class SystemShell(
        val clock: ClockText,
        val battery: String,
        val signal: String,
        val environment: EnvironmentMode,
    )
}
