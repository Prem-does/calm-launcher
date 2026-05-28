package com.calmlauncher.domain.models

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val openDelaySeconds: Int = 0
)
