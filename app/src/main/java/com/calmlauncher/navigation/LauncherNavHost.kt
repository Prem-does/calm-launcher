package com.calmlauncher.navigation

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.calmlauncher.domain.models.AppLaunchRequest
import com.calmlauncher.launcher.HomeRoleManager
import com.calmlauncher.launcher.LauncherStateViewModel
import com.calmlauncher.launcher.*
import com.calmlauncher.launcher.shouldPromptForHomeRole
import com.calmlauncher.security.DevicePolicyEnforcer
import com.calmlauncher.ui.screens.AppListScreen
import com.calmlauncher.ui.screens.FocusModeScreen
import com.calmlauncher.ui.screens.HomeScreen
import com.calmlauncher.ui.screens.LaunchGateScreen
import com.calmlauncher.ui.screens.SearchScreen
import com.calmlauncher.ui.screens.SettingsScreen
import com.calmlauncher.ui.screens.ToolScreen

@Composable
fun LauncherNavHost(launcherStateViewModel: LauncherStateViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val homeRoleManager = remember { HomeRoleManager(context) }
    val uiState = launcherStateViewModel.uiState.collectAsState().value
    val prefs = uiState.settings.preferences
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    DisposableEffect(uiState.settings) {
        DevicePolicyEnforcer.apply(context, uiState.settings)
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (uiState.settings.hiddenStatusBar || uiState.settings.kioskModeEnabled || uiState.settings.shouldLockLauncherExit()) {
                controller.hide(android.view.WindowInsets.Type.statusBars())
                controller.hide(android.view.WindowInsets.Type.navigationBars())
                window.setDecorFitsSystemWindows(false)
            } else {
                controller.show(android.view.WindowInsets.Type.statusBars())
                controller.show(android.view.WindowInsets.Type.navigationBars())
                window.setDecorFitsSystemWindows(true)
            }
            window.attributes = window.attributes.apply {
                screenBrightness = when {
                    uiState.settings.useExtraDim() -> 0.15f
                    uiState.settings.nightSchedule() -> 0.3f
                    else -> WindowManagerLayoutParamsBrightnessReset
                }
            }
        }
        audioManager?.let { manager ->
            if (uiState.settings.shouldSilentModeDefault()) {
                // Switching to silent/vibrate touches DND policy, which requires
                // ACCESS_NOTIFICATION_POLICY granted by the user, or setRingerMode throws.
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                    runCatching { manager.ringerMode = AudioManager.RINGER_MODE_SILENT }
                }
            }
        }
        if (uiState.settings.kioskModeEnabledPref() || uiState.settings.preventLauncherExit()) {
            runCatching { activity?.startLockTask() }
        } else {
            runCatching { activity?.stopLockTask() }
        }
        onDispose { }
    }

    LaunchedEffect(Unit) {
        if (activity != null && uiState.settings.shouldPromptForHomeRole() && homeRoleManager.shouldPromptForDefaultHome()) {
            homeRoleManager.requestDefaultHome(activity)
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                settings = uiState.settings,
                recentApps = uiState.recentApps,
                calmInsight = uiState.calmInsight,
                dopamineRiskMessage = uiState.dopamineRiskMessage,
                unlockCount = uiState.unlockCount,
                onOpenApps = { navController.navigate(Routes.APPS) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenFocus = { navController.navigate(Routes.FOCUS) },
                onOpenNotes = { navController.navigate(toolRoute("Notes")) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onLaunchApp = { item ->
                    val deadEndFeed = launcherStateViewModel.recordAppLaunch(item.label)
                    navController.navigate(
                        launchRoute(
                            item.packageName,
                            item.label,
                            if (deadEndFeed) 0 else uiState.settings.appOpeningDelaySeconds(item.label),
                            if (deadEndFeed) false else uiState.settings.requiresLaunchConfirmation(item.label),
                            if (deadEndFeed) true else uiState.settings.isLaunchBlocked(item.label, uiState.todayScreenTimeMinutes),
                            deadEndFeed
                        )
                    )
                },
                onLongPressFocus = {
                    launcherStateViewModel.toggleFocusMode()
                    navController.navigate(Routes.FOCUS)
                },
                onSetPreference = { key, value -> launcherStateViewModel.setPreference(key, value) },
                screenTimeMinutes = uiState.todayScreenTimeMinutes,
                focusModeEnabled = uiState.settings.focusModeEnabled
            )
        }
        composable(Routes.APPS) { AppListScreen(settings = uiState.settings, focusModeEnabled = uiState.settings.focusModeEnabled, onBack = { navController.popBackStack() }, onLaunchApp = { item ->
            val deadEndFeed = launcherStateViewModel.recordAppLaunch(item.label)
            navController.navigate(
                launchRoute(
                    item.packageName,
                    item.label,
                    if (deadEndFeed) 0 else uiState.settings.appOpeningDelaySeconds(item.label),
                    if (deadEndFeed) false else uiState.settings.requiresLaunchConfirmation(item.label),
                    if (deadEndFeed) true else uiState.settings.isLaunchBlocked(item.label, uiState.todayScreenTimeMinutes),
                    deadEndFeed
                )
            )
        }) }
        composable(Routes.SEARCH) {
            SearchScreen(
                settings = uiState.settings,
                recentApps = uiState.recentApps,
                onBack = { navController.popBackStack() },
                onLaunchApp = { item ->
                    val deadEndFeed = launcherStateViewModel.recordAppLaunch(item.label)
                    navController.navigate(
                        launchRoute(
                            item.packageName,
                            item.label,
                            if (deadEndFeed) 0 else uiState.settings.appOpeningDelaySeconds(item.label),
                            if (deadEndFeed) false else uiState.settings.requiresLaunchConfirmation(item.label),
                            if (deadEndFeed) true else uiState.settings.isLaunchBlocked(item.label, uiState.todayScreenTimeMinutes),
                            deadEndFeed
                        )
                    )
                }
            )
        }
        composable(Routes.FOCUS) {
            FocusModeScreen(
                settings = uiState.settings,
                onExit = {
                    launcherStateViewModel.toggleFocusMode()
                    navController.popBackStack()
                },
                focusModeEnabled = uiState.settings.focusModeEnabled,
                onEmergencyBypass = {
                    launcherStateViewModel.emergencyBypassFocusMode()
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                state = uiState.settings,
                todayScreenTimeMinutes = uiState.todayScreenTimeMinutes,
                calmInsight = uiState.calmInsight,
                onSetPin = { pin -> launcherStateViewModel.setPin(pin) },
                onUnlock = { pin -> launcherStateViewModel.unlockSettings(pin) },
                onLock = { launcherStateViewModel.lockSettings() },
                onToggleGrayscale = { launcherStateViewModel.toggleGrayscale() },
                onToggleKioskMode = { launcherStateViewModel.toggleKioskMode() },
                onToggleHiddenStatusBar = { launcherStateViewModel.toggleHiddenStatusBar() },
                onSetPreference = { key, value -> launcherStateViewModel.setPreference(key, value) },
                onTogglePreference = { key -> launcherStateViewModel.togglePreference(key) },
                onToggleFocusMode = { launcherStateViewModel.toggleFocusMode() }
            )
        }
        composable(
            route = Routes.LAUNCH,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("label") { type = NavType.StringType },
                navArgument("delaySeconds") { type = NavType.IntType },
                navArgument("confirm") { type = NavType.BoolType },
                    navArgument("blocked") { type = NavType.BoolType },
                    navArgument("deadEnd") { type = NavType.BoolType }
            )
        ) { entry ->
            val request = AppLaunchRequest(
                packageName = entry.arguments?.getString("packageName").orEmpty(),
                label = entry.arguments?.getString("label").orEmpty(),
                delaySeconds = entry.arguments?.getInt("delaySeconds") ?: 0,
                requiresConfirmation = entry.arguments?.getBoolean("confirm") == true,
                blocked = entry.arguments?.getBoolean("blocked") == true,
                deadEndFeed = entry.arguments?.getBoolean("deadEnd") == true
            )
            LaunchGateScreen(
                request = request,
                settings = uiState.settings,
                onRecordLaunchReason = { packageName, label, reason -> launcherStateViewModel.recordLaunchReason(packageName, label, reason) },
                onDone = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        composable(
            route = Routes.TOOL,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            ToolScreen(
                name = entry.arguments?.getString("name") ?: "Tool",
                settings = uiState.settings,
                screenTimeMinutes = uiState.todayScreenTimeMinutes,
                unlockCount = uiState.unlockCount,
                calmInsight = uiState.calmInsight,
                dopamineRiskMessage = uiState.dopamineRiskMessage,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun launchRoute(packageName: String, label: String, delaySeconds: Int, confirm: Boolean, blocked: Boolean, deadEndFeed: Boolean): String {
    return "launch/${Uri.encode(packageName)}/${Uri.encode(label)}/$delaySeconds/$confirm/$blocked/$deadEndFeed"
}

private const val WindowManagerLayoutParamsBrightnessReset = -1f

private fun toolRoute(name: String): String {
    return Routes.TOOL.replace("{name}", Uri.encode(name))
}
