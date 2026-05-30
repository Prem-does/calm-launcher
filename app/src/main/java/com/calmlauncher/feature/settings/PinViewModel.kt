package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives PIN Protection. Reflects whether a PIN is currently set and brokers the two mutations to
 * the [PinManager]: setting a new PIN, and removing the existing one (gated behind a verify of the
 * entered current PIN). This is a friction lock, not a security boundary, so the surface stays
 * deliberately simple.
 */
@HiltViewModel
class PinViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val pinManager: PinManager,
) : ViewModel() {

    val pinEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.pinEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    /** Set (or replace) the PIN with [pin]; flips [pinEnabled] on once persisted. */
    fun setPin(pin: String) {
        if (pin.isBlank()) return
        viewModelScope.launch { pinManager.setPin(pin) }
    }

    /**
     * Verify [current] and, on success, clear the PIN. [onResult] reports whether the PIN was
     * removed so the screen can surface a "wrong PIN" hint without leaking the stored value.
     */
    fun removePin(current: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = pinManager.verify(current)
            if (ok) pinManager.clearPin()
            onResult(ok)
        }
    }
}
