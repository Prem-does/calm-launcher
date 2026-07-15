package com.calmlauncher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calmlauncher.domain.repository.ReflectionRepository
import com.calmlauncher.domain.usecase.BuildReflectionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.ZoneId

/** Prepares the nightly Usage Reflection prompt for the current day. */
@HiltWorker
class NightlyReflectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val buildReflection: BuildReflectionUseCase,
    private val reflectionRepository: ReflectionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return runCatching {
            reflectionRepository.upsert(buildReflection(dayStart))
        }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}
