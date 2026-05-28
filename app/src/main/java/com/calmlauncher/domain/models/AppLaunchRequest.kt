package com.calmlauncher.domain.models

data class AppLaunchRequest(
    val packageName: String,
    val label: String,
    val delaySeconds: Int = 0,
    val requiresConfirmation: Boolean = false,
    val blocked: Boolean = false
    ,
    val deadEndFeed: Boolean = false
)
