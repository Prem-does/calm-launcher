package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.models.AppEntry

class FilterAppsUseCase {
    fun invoke(installedApps: List<AppEntry>, allowedPackages: Set<String>): List<AppEntry> {
        return installedApps.filter { app -> allowedPackages.contains(app.packageName) }
    }
}
