package com.calmlauncher.data.repository

import com.calmlauncher.domain.models.AppEntry

interface AppRepository {
    fun getAllowedApps(): List<AppEntry>
}
