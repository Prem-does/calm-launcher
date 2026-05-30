package com.calmlauncher.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.accessibility.FocusBlockAccessibilityService
import com.calmlauncher.accessibility.PlatformGuardPolicy
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.domain.service.SystemActions
import com.calmlauncher.launcher.HomeRoleManager
import com.calmlauncher.work.CalmWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Snapshot of the system permissions / roles the launcher wants set up. Each flag is a
 * point-in-time read of an OS surface (role, app-op, power exemption, accessibility
 * binding) rather than an observable stream, so the screen re-pulls it via
 * [OnboardingViewModel.refreshStatus] whenever it resumes from system settings.
 *
 * @property isDefaultHome Calm currently holds the ROLE_HOME / default-launcher role.
 * @property hasUsageAccess Usage-stats access granted (screen-time surfaces).
 * @property batteryExempt Calm is ignoring battery optimizations — critical on Samsung
 *  One UI, which otherwise sleeps the focus/grayscale services.
 * @property focusServiceOn The [FocusBlockAccessibilityService] is enabled (optional).
 */
data class OnboardingUiState(
    val isDefaultHome: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val batteryExempt: Boolean = false,
    val focusServiceOn: Boolean = false,
)

/**
 * Drives the first-run onboarding. Holds no cold flows: the permission states are
 * imperative OS reads recomputed on demand by [refreshStatus] (called when the screen
 * resumes after the user returns from a system settings deep-link). The deep-link helpers
 * delegate straight to [SystemActions]; [complete] persists the onboarding flag and kicks
 * off the periodic background work via [CalmWorkScheduler].
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val homeRoleManager: HomeRoleManager,
    val systemActions: SystemActions,
    private val settingsRepository: SettingsRepository,
    private val workScheduler: CalmWorkScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(currentStatus())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Re-reads every system surface and republishes [uiState]. */
    fun refreshStatus() {
        _uiState.value = currentStatus()
    }

    private fun currentStatus(): OnboardingUiState = OnboardingUiState(
        isDefaultHome = homeRoleManager.isDefaultHome(),
        hasUsageAccess = PlatformGuardPolicy.hasUsageAccess(context),
        batteryExempt = PlatformGuardPolicy.isIgnoringBatteryOptimizations(context),
        focusServiceOn = PlatformGuardPolicy.isAccessibilityServiceEnabled(
            context,
            FocusBlockAccessibilityService::class.java,
        ),
    )

    fun openUsageAccess() = systemActions.openUsageAccessSettings()

    fun requestBattery() = systemActions.requestIgnoreBatteryOptimizations()

    fun openAccessibility() = systemActions.openAccessibilitySettings()

    /** Mark onboarding done and schedule the launcher's periodic work. */
    fun complete() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(onboardingComplete = true) }
            workScheduler.scheduleAll()
        }
    }
}
