package com.calmlauncher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.calmlauncher.data.system.LauncherAppCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppListViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow(LauncherAppCatalog.loadAllowedApps(application))
    val apps: StateFlow<List<com.calmlauncher.data.system.LauncherAppItem>> = _apps.asStateFlow()
}
