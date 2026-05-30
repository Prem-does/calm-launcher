package com.calmlauncher.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Current battery level (0..100) and whether the device is charging. */
data class BatteryStatus(
    val percent: Int,
    val charging: Boolean,
)

/**
 * Observes battery level/charging via a registered [BroadcastReceiver] for
 * ACTION_BATTERY_CHANGED. The sticky broadcast returned at registration time gives an
 * immediate first value; the receiver is unregistered when collection stops.
 */
@Singleton
class BatteryObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val status: Flow<BatteryStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { trySend(it.toBatteryStatus()) }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // registerReceiver returns the current sticky Intent → emit an initial value.
        val sticky = runCatching { context.registerReceiver(receiver, filter) }.getOrNull()
        sticky?.let { trySend(it.toBatteryStatus()) }
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }.conflate().distinctUntilChanged()

    private fun Intent.toBatteryStatus(): BatteryStatus {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val statusExtra = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = statusExtra == BatteryManager.BATTERY_STATUS_CHARGING ||
            statusExtra == BatteryManager.BATTERY_STATUS_FULL
        return BatteryStatus(percent = percent.coerceIn(0, 100), charging = charging)
    }
}
