package com.calmlauncher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Legacy one-shot work kept only to retire existing scheduled work safely.
 *
 * A worker cannot legally present a focusable screen over another app. Foreground enforcement now
 * lives in FocusBlockAccessibilityService, which can identify the visible package and attach the
 * input-owning blocking screen. Do not send HOME from here: that would replace the explanation
 * with an unexplained bounce.
 */
@HiltWorker
class AppLimitEnforceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = Result.success()
}
