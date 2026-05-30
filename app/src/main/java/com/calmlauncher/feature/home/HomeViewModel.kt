package com.calmlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.core.util.TimeFormatter
import com.calmlauncher.data.system.BatteryObserver
import com.calmlauncher.data.system.ClockTicker
import com.calmlauncher.data.system.ConnectivityObserver
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.LaunchSource
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.usecase.BuildInsightsUseCase
import com.calmlauncher.domain.usecase.ObserveRestrictionStateUseCase
import com.calmlauncher.launcher.LaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    appRepository: AppRepository,
    buildInsights: BuildInsightsUseCase,
    observeRestriction: ObserveRestrictionStateUseCase,
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

    private val topInsight = buildInsights().map { it.firstOrNull()?.text }

    // combine() takes at most 5 flows directly; group the system streams into one combine,
    // then fold that together with the remaining flows in an outer combine.
    private val system = combine(
        clock,
        battery,
        connectivityObserver.signal,
    ) { clockText, batteryText, signalText ->
        Triple(clockText, batteryText, signalText)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        system,
        appRepository.observeFavorites(),
        topInsight,
        observeRestriction(),
    ) { (clockText, batteryText, signalText), favorites, insight, restriction ->
        HomeUiState(
            time = clockText.time,
            date = clockText.date,
            batteryText = batteryText,
            signalText = signalText,
            favorites = favorites,
            insight = insight,
            restriction = restriction,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
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

    private data class ClockText(val time: String, val date: String)
}
