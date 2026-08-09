package com.calmlauncher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calmlauncher.domain.repository.RoutineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RoutineRolloverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val routineRepository: RoutineRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        routineRepository.refreshRollover(System.currentTimeMillis())
        return Result.success()
    }
}