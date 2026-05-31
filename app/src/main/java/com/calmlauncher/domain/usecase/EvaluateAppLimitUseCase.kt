package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.repository.AppLimitRepository
import javax.inject.Inject

class EvaluateAppLimitUseCase @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
) {
    suspend operator fun invoke(request: AppLaunchRequest): AppLimitDecision =
        appLimitRepository.evaluate(request.packageName, request.label)
}
