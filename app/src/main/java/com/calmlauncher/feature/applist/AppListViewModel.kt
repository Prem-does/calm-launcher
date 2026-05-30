package com.calmlauncher.feature.applist

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.data.system.BatteryObserver
import com.calmlauncher.data.system.ConnectivityObserver
import com.calmlauncher.data.system.LauncherAppCatalog
import com.calmlauncher.domain.model.AppDisplayMode
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.LaunchSource
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.domain.usecase.FilterAppsUseCase
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
 * Drives the App List — the full drawer of every launchable app. Combines the visible-app
 * catalog with the live settings (applying [FilterAppsUseCase.filter] for hide-social /
 * environment visibility), the display mode, battery/signal status and the UI restriction
 * state into a single [AppListUiState]. App opens are delegated to the central
 * [LaunchCoordinator] so they pass through the friction pipeline, tagged [LaunchSource.APP_LIST].
 */
@HiltViewModel
class AppListViewModel @Inject constructor(
    appRepository: AppRepository,
    settingsRepository: SettingsRepository,
    filterApps: FilterAppsUseCase,
    observeRestriction: ObserveRestrictionStateUseCase,
    batteryObserver: BatteryObserver,
    connectivityObserver: ConnectivityObserver,
    val catalog: LauncherAppCatalog,
    private val launchCoordinator: LaunchCoordinator,
) : ViewModel() {

    private val battery = batteryObserver.status.map { "${it.percent}%" }

    // Visible apps + settings → the filtered, alphabetical drawer plus the display mode.
    private val drawer = combine(
        appRepository.observeVisibleApps(),
        settingsRepository.settings,
    ) { apps, settings ->
        Drawer(
            apps = filterApps.filter(apps, settings),
            displayMode = settings.displayMode,
        )
    }

    val uiState: StateFlow<AppListUiState> = combine(
        drawer,
        battery,
        connectivityObserver.signal,
        observeRestriction(),
    ) { drawerState, batteryText, signalText, restriction ->
        AppListUiState(
            apps = drawerState.apps,
            displayMode = drawerState.displayMode,
            batteryText = batteryText,
            signalText = signalText,
            restriction = restriction,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppListUiState(),
    )

    /** Open an app through the friction pipeline, tagged as launched from the app list. */
    fun open(app: AppEntry) = launchCoordinator.request(
        AppLaunchRequest(
            packageName = app.packageName,
            label = app.label,
            category = app.category,
            source = LaunchSource.APP_LIST,
        ),
    )

    /** Resolve the app icon for [packageName] (ICONS display mode), or null if unavailable. */
    fun iconFor(packageName: String): Drawable? = catalog.loadIcon(packageName)

    private data class Drawer(
        val apps: List<AppEntry>,
        val displayMode: AppDisplayMode,
    )
}
