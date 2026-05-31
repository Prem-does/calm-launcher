package com.calmlauncher.accessibility

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.NotificationEventType
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.repository.AnalyticsRepository
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Suppresses addictive notification surfacing from distracting apps while a focus
 * session is active (or the user has chosen to keep social apps invisible). It cancels
 * the posted notification so it never reaches the shade — a calm, quiet phone. Calls/
 * messages and tool apps are left untouched.
 */
@AndroidEntryPoint
class CalmNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var analyticsRepository: AnalyticsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var settings: LauncherSettings = LauncherSettings()
    @Volatile private var categories: Map<String, AppCategory> = emptyMap()

    override fun onListenerConnected() {
        super.onListenerConnected()
        settingsRepository.settings
            .onEach { settings = it }
            .launchIn(scope)
        appRepository.observeApps()
            .onEach { apps -> categories = apps.associate { it.packageName to it.category } }
            .launchIn(scope)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (pkg != packageName) {
            scope.launch {
                analyticsRepository.recordNotification(
                    packageName = pkg,
                    timestampEpochMs = sbn.postTime,
                    eventType = NotificationEventType.POSTED,
                )
            }
        }
        if (pkg == packageName) return
        val category = categories[pkg] ?: return
        val distracting = category == AppCategory.SOCIAL ||
            category == AppCategory.ENTERTAINMENT ||
            category == AppCategory.GAME
        val shouldSuppress = distracting && (settings.focusActive || settings.hideSocialApps)
        if (shouldSuppress) {
            runCatching { cancelNotification(sbn.key) }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: NotificationListenerService.RankingMap?,
        reason: Int,
    ) {
        val pkg = sbn?.packageName ?: return
        if (pkg == packageName) return
        val eventType = when (reason) {
            REASON_CLICK -> NotificationEventType.OPENED
            REASON_APP_CANCEL,
            REASON_CANCEL,
            REASON_CANCEL_ALL,
            REASON_LISTENER_CANCEL,
            REASON_LISTENER_CANCEL_ALL,
            REASON_PACKAGE_CHANGED,
            REASON_USER_STOPPED -> NotificationEventType.IGNORED
            else -> NotificationEventType.REMOVED
        }
        scope.launch {
            analyticsRepository.recordNotification(
                packageName = pkg,
                timestampEpochMs = System.currentTimeMillis(),
                eventType = eventType,
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
