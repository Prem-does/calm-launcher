package com.calmlauncher.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calmlauncher.data.db.AppLimitDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LimitAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var notifications: LimitNotificationManager
    @Inject lateinit var dao: AppLimitDao

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return
        val remaining = intent.getIntExtra("remainingMinutes", 0)
        val label = packageName
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            try {
                notifications.notifyApproachingLimit(packageName, label, remaining)
                // update lastNotified in DB
                val rule = dao.getRule(packageName)
                if (rule != null) {
                    dao.upsertRule(rule.copy(lastNotifiedEpochMs = now))
                }
            } catch (_: Exception) {
            }
        }
    }
}
