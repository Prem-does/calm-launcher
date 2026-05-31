package com.calmlauncher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calmlauncher.domain.repository.AppLimitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodically refreshes app-limit usage snapshots from foreground usage stats. */
@HiltWorker
class AppLimitRollupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appLimitRepository: AppLimitRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching { appLimitRepository.refreshUsageSnapshot() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}
