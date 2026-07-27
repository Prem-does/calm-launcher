package com.calmlauncher.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calmlauncher.data.db.AppLimitDao
import com.calmlauncher.domain.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires at the precise moment an app is due to cross a limit threshold, which is far more
 * accurate than waiting for the 15-minute usage rollup. Re-checks the rule before posting so
 * a limit the user disabled (or overrode) in the meantime stays quiet.
 */
@AndroidEntryPoint
class LimitAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var notifications: LimitNotificationManager
    @Inject lateinit var dao: AppLimitDao
    @Inject lateinit var appRepository: AppRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        val remaining = intent.getIntExtra(EXTRA_REMAINING_MINUTES, 0)
        val pending = goAsync()

        scope.launch {
            try {
                val rule = dao.getRule(packageName)
                val now = System.currentTimeMillis()
                // The alarm was scheduled minutes ago; the rule may have changed since.
                if (rule == null || !rule.enabled || rule.overrideUntilEpochMs > now) {
                    notifications.clear(packageName)
                    return@launch
                }

                val label = runCatching { appRepository.getApp(packageName)?.label }
                    .getOrNull() ?: packageName
                if (remaining <= 0) {
                    notifications.notifyLimitReached(packageName, label)
                } else {
                    notifications.notifyApproachingLimit(packageName, label, remaining)
                }
                dao.upsertRule(rule.copy(lastNotifiedEpochMs = now))
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
