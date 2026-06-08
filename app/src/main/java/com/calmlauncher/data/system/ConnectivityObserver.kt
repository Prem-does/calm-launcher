package com.calmlauncher.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.provider.Settings
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits a short, human-readable connectivity label: "Wi-Fi", "5G" / "LTE" / "Mobile",
 * "Ethernet", or "Offline". Cellular generation is best-effort — if the OS denies the
 * network-type query (no phone permission on some versions) it degrades to "Mobile".
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val cm: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val signal: Flow<String> = callbackFlow {
        val manager = cm
        if (manager == null) {
            trySend(OFFLINE)
            awaitClose { }
            return@callbackFlow
        }

        var hotspotActive = isHotspotEnabled()

        fun emitCurrent() {
            trySend(currentLabel(manager, hotspotActive))
        }

        val airplaneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_WIFI_AP_STATE_CHANGED) {
                    hotspotActive = isHotspotEnabled(intent)
                }
                emitCurrent()
            }
        }
        val systemFilter = IntentFilter().apply {
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(ACTION_WIFI_AP_STATE_CHANGED)
        }
        context.registerReceiver(airplaneReceiver, systemFilter)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrent()
            override fun onLost(network: Network) = emitCurrent()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) = emitCurrent()
        }

        val registered = runCatching {
            manager.registerDefaultNetworkCallback(callback)
            true
        }.getOrElse {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            runCatching {
                manager.registerNetworkCallback(request, callback)
                true
            }.getOrDefault(false)
        }

        emitCurrent() // initial value

        awaitClose {
            runCatching { context.unregisterReceiver(airplaneReceiver) }
            if (registered) runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }.conflate().distinctUntilChanged()

    private fun currentLabel(manager: ConnectivityManager, hotspotActive: Boolean): String {
        if (isAirplaneModeOn()) return AIRPLANE
        val network = manager.activeNetwork ?: return OFFLINE
        val caps = runCatching { manager.getNetworkCapabilities(network) }.getOrNull()
            ?: return OFFLINE
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return OFFLINE
        val baseLabel = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularLabel()
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Online"
        }
        return if (hotspotActive) "$baseLabel - $HOTSPOT" else baseLabel
    }

    private fun isAirplaneModeOn(): Boolean {
        return runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0,
            ) == 1
        }.getOrDefault(false)
    }

    private fun isHotspotEnabled(intent: Intent? = null): Boolean {
        val broadcastState = intent?.getIntExtra(EXTRA_WIFI_AP_STATE, WIFI_AP_STATE_FAILED)
        if (broadcastState != null && broadcastState != WIFI_AP_STATE_FAILED) {
            return broadcastState == WIFI_AP_STATE_ENABLED
        }
        return runCatching {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
            val method = wifiManager?.javaClass?.getDeclaredMethod("isWifiApEnabled")
            method?.isAccessible = true
            method?.invoke(wifiManager) as? Boolean
        }.getOrNull() ?: false
    }

    private fun cellularLabel(): String {
        val tm = runCatching {
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        }.getOrNull() ?: return MOBILE
        val type = runCatching { tm.dataNetworkType }.getOrNull() ?: return MOBILE
        return when (type) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_CDMA,
            -> "2G"
            else -> MOBILE
        }
    }

    private companion object {
        const val AIRPLANE = "Airplane"
        const val HOTSPOT = "Hotspot"
        const val OFFLINE = "Offline"
        const val MOBILE = "Mobile"
        const val ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        const val EXTRA_WIFI_AP_STATE = "wifi_state"
        const val WIFI_AP_STATE_ENABLED = 13
        const val WIFI_AP_STATE_FAILED = 14
    }
}
