package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.models.AppEntry

class ShouldShowConfirmationUseCase {
    fun invoke(app: AppEntry): Boolean = app.requiresConfirmation
}
