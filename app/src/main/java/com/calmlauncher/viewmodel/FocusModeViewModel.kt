package com.calmlauncher.viewmodel

import androidx.lifecycle.ViewModel
import com.calmlauncher.domain.models.FocusModePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusModeViewModel : ViewModel() {
    private val _policy = MutableStateFlow(FocusModePolicy())
    val policy: StateFlow<FocusModePolicy> = _policy.asStateFlow()
}
