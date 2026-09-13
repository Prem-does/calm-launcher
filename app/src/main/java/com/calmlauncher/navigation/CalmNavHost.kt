package com.calmlauncher.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calmlauncher.core.designsystem.grayscale
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmTheme
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.feature.applist.AppListScreen
import com.calmlauncher.feature.limits.AppLimitsScreen
import com.calmlauncher.feature.focus.FocusEntryTransition
import com.calmlauncher.feature.gate.LaunchGateHost
import com.calmlauncher.feature.home.HomeScreen
import com.calmlauncher.feature.onboarding.OnboardingScreen
import com.calmlauncher.feature.reflection.ReflectionScreen
import com.calmlauncher.feature.reminders.RemindersScreen
import com.calmlauncher.feature.reset.DeadEndResetScreen
import com.calmlauncher.feature.search.SearchScreen
import com.calmlauncher.feature.settings.CustomizationScreen
import com.calmlauncher.feature.settings.EnvironmentScreen
import com.calmlauncher.feature.settings.FrictionScreen
import com.calmlauncher.feature.settings.ManageAppsScreen
import com.calmlauncher.feature.settings.ScreenTimeScreen
import com.calmlauncher.feature.settings.SettingsScreen
import com.calmlauncher.feature.settings.ThemeViewModel

/**
 * The composition root. Resolves the start destination from onboarding state, hosts the
 * navigation graph, and layers the launch gate above everything so friction overlays can
 * cover any screen.
 */
@Composable
fun CalmRoot(rootViewModel: RootViewModel = hiltViewModel()) {
    val onboardingComplete by rootViewModel.onboardingComplete.collectAsStateWithLifecycle()
    val appCatalogReady by rootViewModel.appCatalogReady.collectAsStateWithLifecycle()
    val themeViewModel: ThemeViewModel = hiltViewModel()
    // The full appearance, not just light/dark: collecting it here is what makes every
    // Customization change apply on the next frame, with no launcher restart.
    val appearance by themeViewModel.appearance.collectAsStateWithLifecycle()
    val restriction by rootViewModel.restriction.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    CalmTheme(appearance = appearance) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CalmBlack)
                .grayscale(restriction.grayscale, restriction.grayscaleAmount),
        ) {
            val complete = onboardingComplete
            if (complete == null) {
                LoadingRootSurface()
            } else if (complete && !appCatalogReady) {
                LoadingRootSurface()
            } else {
                CalmNavHost(
                    navController = navController,
                    startDestination = if (complete) Routes.HOME else Routes.ONBOARDING,
                )
            }
            // Sits above the nav host; renders nothing unless a launch is being gated.
            LaunchGateHost(onNavigateToReset = { navController.navigate(Routes.RESET) })
        }
    }
}

@Composable
private fun LoadingRootSurface(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-pulse-alpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.marginMobile),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "CALM",
            style = CalmType.headlineLgMobile,
            color = CalmWhite,
        )
        Spacer(Modifier.height(Spacing.stackMd))
        CircularProgressIndicator(
            modifier = Modifier
                .height(28.dp)
                .alpha(pulse),
            color = CalmWhite,
            strokeWidth = 1.5.dp,
        )
        Text(
            text = "Preparing your space",
            style = CalmType.labelMd,
            color = CalmGray,
            modifier = Modifier
                .padding(top = Spacing.stackMd)
                .alpha(pulse),
        )
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
    val back: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val closeSearch: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
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
                onOpenSearch = {
                    navController.navigate(Routes.SEARCH) {
                        launchSingleTop = true
                    }
                },
                onOpenEnvironment = { navController.navigate(Routes.SETTINGS_ENVIRONMENT) },
            )
        }

        composable(Routes.APPS) {
            AppListScreen(
                onSelectTab = selectTab,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenEnvironment = { navController.navigate(Routes.SETTINGS_ENVIRONMENT) },
            )
        }

        composable(Routes.FOCUS) {
            FocusEntryTransition(onExit = { selectTab(Routes.HOME) })
        }

        composable(Routes.SEARCH) {
            SearchScreen(onClose = closeSearch)
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
                onOpenAppLimits = { navController.navigate(Routes.SETTINGS_APP_LIMITS) },
                onOpenFriction = { navController.navigate(Routes.SETTINGS_FRICTION) },
                onOpenEnvironment = { navController.navigate(Routes.SETTINGS_ENVIRONMENT) },
                onOpenReflection = { navController.navigate(Routes.REFLECTION) },
                onOpenReminders = { navController.navigate(Routes.SETTINGS_REMINDERS) },
                onOpenCustomization = { navController.navigate(Routes.SETTINGS_CUSTOMIZATION) },
            )
        }

        composable(Routes.SETTINGS_MANAGE_APPS) { ManageAppsScreen(onBack = back) }
        composable(Routes.SETTINGS_SCREEN_TIME) { ScreenTimeScreen(onBack = back) }
        composable(Routes.SETTINGS_APP_LIMITS) { AppLimitsScreen(onBack = back) }
        composable(Routes.SETTINGS_FRICTION) { FrictionScreen(onBack = back) }
        composable(Routes.SETTINGS_ENVIRONMENT) { EnvironmentScreen(onBack = back) }
        composable(Routes.SETTINGS_REMINDERS) { RemindersScreen(onBack = back) }
        composable(Routes.SETTINGS_CUSTOMIZATION) { CustomizationScreen(onBack = back) }
    }
}
