package com.calmlauncher.accessibility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calmlauncher.domain.repository.AnalyticsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AnalyticsEventReceiver : BroadcastReceiver() {

    @Inject lateinit var analyticsRepository: AnalyticsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            scope.launch {
                analyticsRepository.recordUnlock(System.currentTimeMillis())
            }
        }
    }
}
