package com.calmlauncher.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calmlauncher.domain.repository.AppLimitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires at the precise moment an app is due to cross a limit threshold, which is far more
 * accurate than waiting for the 15-minute usage rollup.
 *
 * It deliberately posts nothing itself. The alarm is only a *prompt to re-check*: it hands off
 * to [AppLimitRepository.syncLimitNotification], which reads real usage, compares it against
 * what the user has already been told today, and stays quiet when that is nothing new. Four
 * alarms plus a rollup all landing in the same minute therefore produce at most one
 * notification — and a limit disabled or overridden since the alarm was scheduled produces
 * none at all.
 */
@AndroidEntryPoint
class LimitAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var appLimitRepository: AppLimitRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        val pending = goAsync()

        scope.launch {
            try {
                appLimitRepository.syncLimitNotification(packageName)
            } catch (_: Exception) {
                // Best-effort: a failed notification must not crash the receiver.
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "packageName"
        const val EXTRA_REMAINING_MINUTES = "remainingMinutes"
    }
}
