package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives Manage Apps. Exposes the full app catalog (including hidden apps, so they stay
 * reachable by deliberate intent) and forwards per-app metadata mutations — hide, favourite
 * and category — to the [AppRepository] on the view-model scope.
 */
@HiltViewModel
class ManageAppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {

    val apps: StateFlow<List<AppEntry>> = appRepository.observeApps().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    fun setHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch { appRepository.setHidden(packageName, hidden) }
    }

    fun setFavorite(packageName: String, favorite: Boolean) {
        viewModelScope.launch { appRepository.setFavorite(packageName, favorite) }
    }

    /** Advance an app to the next [AppCategory] (wraps), so the label cycles on each tap. */
    fun cycleCategory(app: AppEntry) {
        val values = AppCategory.entries
        val next = values[(app.category.ordinal + 1) % values.size]
        viewModelScope.launch { appRepository.setCategory(app.packageName, next) }
    }
}
