package com.calmlauncher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calmlauncher.domain.repository.ScreenTimeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodically pulls foreground usage into the local screen-time store. */
@HiltWorker
class ScreenTimeRollupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val screenTimeRepository: ScreenTimeRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { screenTimeRepository.refresh() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}
