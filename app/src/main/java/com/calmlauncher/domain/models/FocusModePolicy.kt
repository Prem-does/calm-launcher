package com.calmlauncher.domain.models

data class FocusModePolicy(
    val enabled: Boolean = false,
    val emergencyBypassAllowed: Boolean = true,
    val blankScreen: Boolean = false,
    val timerMinutes: Int? = null
)
