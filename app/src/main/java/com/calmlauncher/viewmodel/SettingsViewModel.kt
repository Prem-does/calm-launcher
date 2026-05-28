package com.calmlauncher.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsState(
    val pinProtected: Boolean = true,
    val grayscaleForced: Boolean = true,
    val kioskModeEnabled: Boolean = false,
    val hiddenStatusBar: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
}
