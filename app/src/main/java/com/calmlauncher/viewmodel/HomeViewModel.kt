package com.calmlauncher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.usecase.BuildHomeStateUseCase
import com.calmlauncher.domain.usecase.HomeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val buildHomeStateUseCase: BuildHomeStateUseCase = BuildHomeStateUseCase()
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(
        buildHomeStateUseCase.invoke("12:00", "Mon, Jan 1", "100%", "Wi-Fi")
    )
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = buildHomeStateUseCase.invoke("12:00", "Mon, Jan 1", "100%", "Wi-Fi")
        }
    }
}
