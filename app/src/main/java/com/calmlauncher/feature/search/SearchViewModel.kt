package com.calmlauncher.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.LaunchSource
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.launcher.LaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Search screen — the deliberate way to reach any app, including hidden/social ones.
 * Holds the live query and resolves matches via [AppRepository.search], which *intentionally*
 * includes hidden apps so the Minimal Social Layer stays reachable by explicit intent. Opens go
 * through the [LaunchCoordinator], tagged [LaunchSource.SEARCH].
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val launchCoordinator: LaunchCoordinator,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Live search results for the current [query]; includes hidden apps by design. */
    val results: StateFlow<List<AppEntry>> = combine(
        _query,
        appRepository.observeApps(),
    ) { q, apps ->
        apps.search(q)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /** Update the search query; results recompute off the latest value. */
    fun onQuery(q: String) {
        _query.value = q
    }

    /**
     * Pin or unpin a result. Search is the one surface that lists *every* app, hidden and
     * favourited alike, so it is where an app can always be un-favourited from.
     */
    fun setFavorite(app: AppEntry, favorite: Boolean) {
        viewModelScope.launch { appRepository.setFavorite(app.packageName, favorite) }
    }

    /** Open a result through the friction pipeline, tagged as launched from search. */
    fun open(app: AppEntry) = launchCoordinator.request(
        AppLaunchRequest(
            packageName = app.packageName,
            label = app.label,
            category = app.category,
            source = LaunchSource.SEARCH,
        ),
    )

    private fun List<AppEntry>.search(query: String): List<AppEntry> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return this

        return filter { app ->
            app.label.lowercase().contains(needle) ||
                app.packageName.lowercase().contains(needle)
        }.sortedWith(
            compareByDescending<AppEntry> { it.label.lowercase().startsWith(needle) }
                .thenBy { it.label.lowercase() },
        )
    }
}
