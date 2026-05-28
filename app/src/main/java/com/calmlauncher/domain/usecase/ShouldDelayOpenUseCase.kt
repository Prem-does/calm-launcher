package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.models.AppEntry

class ShouldDelayOpenUseCase {
    fun invoke(app: AppEntry): Int = app.openDelaySeconds
}
