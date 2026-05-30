package com.calmlauncher.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calmlauncher.core.designsystem.grayscale
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.feature.applist.AppListScreen
import com.calmlauncher.feature.focus.FocusScreen
import com.calmlauncher.feature.gate.LaunchGateHost
import com.calmlauncher.feature.home.HomeScreen
import com.calmlauncher.feature.onboarding.OnboardingScreen
import com.calmlauncher.feature.reflection.ReflectionScreen
import com.calmlauncher.feature.reset.DeadEndResetScreen
import com.calmlauncher.feature.search.SearchScreen
import com.calmlauncher.feature.settings.EnvironmentScreen
import com.calmlauncher.feature.settings.FrictionScreen
import com.calmlauncher.feature.settings.ManageAppsScreen
import com.calmlauncher.feature.settings.PinScreen
import com.calmlauncher.feature.settings.ScreenTimeScreen
import com.calmlauncher.feature.settings.SettingsScreen

/**
 * The composition root. Resolves the start destination from onboarding state, hosts the
 * navigation graph, and layers the launch gate above everything so friction overlays can
 * cover any screen.
 */
@Composable
fun CalmRoot(rootViewModel: RootViewModel = hiltViewModel()) {
    val onboardingComplete by rootViewModel.onboardingComplete.collectAsStateWithLifecycle()
    val restriction by rootViewModel.restriction.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmBlack)
            .grayscale(restriction.grayscale, restriction.grayscaleAmount),
    ) {
        val complete = onboardingComplete
        if (complete != null) {
            val navController = rememberNavController()
            CalmNavHost(
                navController = navController,
                startDestination = if (complete) Routes.HOME else Routes.ONBOARDING,
            )
            // Sits above the nav host; renders nothing unless a launch is being gated.
            LaunchGateHost(onNavigateToReset = { navController.navigate(Routes.RESET) })
        }
    }
}

@Composable
fun CalmNavHost(
    navController: NavHostController,
    startDestination: String,
) {
    // Bottom-nav tab switching, anchored on HOME with saved/restored state.
    val selectTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val back: () -> Unit = { navController.popBackStack() }
    val goHome: () -> Unit = {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.HOME) {
            // Home is the root surface — swallow back so we never leave the launcher.
            BackHandler(enabled = true) {}
            HomeScreen(
                onSelectTab = selectTab,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
            )
        }

        composable(Routes.APPS) {
            AppListScreen(
                onSelectTab = selectTab,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.FOCUS) {
            FocusScreen(onExit = { selectTab(Routes.HOME) })
        }

        composable(Routes.SEARCH) {
            SearchScreen(onClose = back)
        }

        composable(Routes.REFLECTION) {
            ReflectionScreen(onBack = back)
        }

        composable(Routes.RESET) {
            DeadEndResetScreen(onDone = goHome)
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = back,
                onOpenManageApps = { navController.navigate(Routes.SETTINGS_MANAGE_APPS) },
                onOpenScreenTime = { navController.navigate(Routes.SETTINGS_SCREEN_TIME) },
                onOpenFriction = { navController.navigate(Routes.SETTINGS_FRICTION) },
                onOpenEnvironment = { navController.navigate(Routes.SETTINGS_ENVIRONMENT) },
                onOpenPin = { navController.navigate(Routes.SETTINGS_PIN) },
                onOpenReflection = { navController.navigate(Routes.REFLECTION) },
            )
        }

        composable(Routes.SETTINGS_MANAGE_APPS) { ManageAppsScreen(onBack = back) }
        composable(Routes.SETTINGS_SCREEN_TIME) { ScreenTimeScreen(onBack = back) }
        composable(Routes.SETTINGS_FRICTION) { FrictionScreen(onBack = back) }
        composable(Routes.SETTINGS_ENVIRONMENT) { EnvironmentScreen(onBack = back) }
        composable(Routes.SETTINGS_PIN) { PinScreen(onBack = back) }
    }
}
