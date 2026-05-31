package com.calmlauncher.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the launcher's periodic background work. Idempotent — safe to call on every
 * launch (uses KEEP so existing schedules are preserved).
 */
@Singleton
class CalmWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleAll() {
        val wm = WorkManager.getInstance(context)

        wm.enqueueUniqueWork(
            SCREEN_TIME_BOOTSTRAP,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ScreenTimeRollupWorker>().build(),
        )

        wm.enqueueUniqueWork(
            APP_LIMITS_BOOTSTRAP,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AppLimitRollupWorker>().build(),
        )

        wm.enqueueUniqueWork(
            ANALYTICS_BOOTSTRAP,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AnalyticsWorker>().build(),
        )

        wm.enqueueUniquePeriodicWork(
            SCREEN_TIME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ScreenTimeRollupWorker>(2, TimeUnit.HOURS).build(),
        )

        wm.enqueueUniquePeriodicWork(
            APP_LIMITS,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AppLimitRollupWorker>(15, TimeUnit.MINUTES).build(),
        )

        wm.enqueueUniquePeriodicWork(
            ANALYTICS,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AnalyticsWorker>(1, TimeUnit.HOURS).build(),
        )

        wm.enqueueUniquePeriodicWork(
            RISK_EVAL,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RiskEvaluationWorker>(30, TimeUnit.MINUTES).build(),
        )

        wm.enqueueUniquePeriodicWork(
            REFLECTION,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NightlyReflectionWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialNightDelayMillis(), TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    /** Milliseconds until the next ~22:00 local time. */
    private fun initialNightDelayMillis(): Long {
        val zone = ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        var next = LocalDate.now(zone).atTime(LocalTime.of(22, 0)).atZone(zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return java.time.Duration.between(now, next).toMillis().coerceAtLeast(0L)
    }

    private companion object {
        const val SCREEN_TIME_BOOTSTRAP = "calm_screen_time_bootstrap"
        const val SCREEN_TIME = "calm_screen_time_rollup"
        const val APP_LIMITS_BOOTSTRAP = "calm_app_limits_bootstrap"
        const val APP_LIMITS = "calm_app_limits_rollup"
        const val ANALYTICS_BOOTSTRAP = "calm_analytics_bootstrap"
        const val ANALYTICS = "calm_analytics_rollup"
        const val RISK_EVAL = "calm_risk_evaluation"
        const val REFLECTION = "calm_nightly_reflection"
    }
}
