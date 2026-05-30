package com.calmlauncher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calmlauncher.domain.usecase.EvaluateRiskUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodically re-runs the Dopamine Detection Engine and persists the risk tier. */
@HiltWorker
class RiskEvaluationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val evaluateRisk: EvaluateRiskUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { evaluateRisk() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}
