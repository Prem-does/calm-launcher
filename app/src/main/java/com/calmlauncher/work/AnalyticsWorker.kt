package com.calmlauncher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calmlauncher.domain.repository.AnalyticsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AnalyticsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analyticsRepository: AnalyticsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { analyticsRepository.refresh() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}
