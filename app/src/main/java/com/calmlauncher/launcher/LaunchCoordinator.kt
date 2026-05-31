package com.calmlauncher.launcher

import com.calmlauncher.di.ApplicationScope
import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.FrictionStep
import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.service.AppLauncher
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.usecase.RecordLaunchUseCase
import com.calmlauncher.domain.usecase.EvaluateAppLimitUseCase
import com.calmlauncher.domain.usecase.ResolveLaunchDecisionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** The currently-displayed friction sequence, or null when no gate is showing. */
data class LaunchFlowState(
    val request: AppLaunchRequest,
    val steps: List<FrictionStep>,
    val index: Int,
) {
    val current: FrictionStep? get() = steps.getOrNull(index)
}

/** One-shot effects the root reacts to (navigate to reset, show a blocked message). */
sealed interface LaunchEffect {
    data class Blocked(val reason: String) : LaunchEffect
    data class DeadEnd(val request: AppLaunchRequest) : LaunchEffect
    data class Launched(val packageName: String) : LaunchEffect
    data class AppLimitBlocked(val request: AppLaunchRequest, val status: AppLimitStatus) : LaunchEffect
}

/**
 * Central launch state machine. Every app open in the launcher goes through here:
 * the request is resolved by the ModeEngine into an ordered [LaunchDecision], and the
 * gate host walks the resulting [FrictionStep]s before the app is actually opened and
 * the event recorded. Allow-decisions launch immediately.
 */
@Singleton
class LaunchCoordinator @Inject constructor(
    private val evaluateAppLimit: EvaluateAppLimitUseCase,
    private val appLimitRepository: AppLimitRepository,
    private val resolveLaunchDecision: ResolveLaunchDecisionUseCase,
    private val recordLaunch: RecordLaunchUseCase,
    private val appLauncher: AppLauncher,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _flow = MutableStateFlow<LaunchFlowState?>(null)
    val flow: StateFlow<LaunchFlowState?> = _flow.asStateFlow()

    private val _effects = MutableSharedFlow<LaunchEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<LaunchEffect> = _effects.asSharedFlow()

    /** Entry point for any UI that wants to open an app. */
    fun request(request: AppLaunchRequest) {
        scope.launch {
            when (val limitDecision = evaluateAppLimit(request)) {
                is com.calmlauncher.domain.model.AppLimitDecision.Blocked -> {
                    _effects.emit(LaunchEffect.AppLimitBlocked(request, limitDecision.status))
                    return@launch
                }
                com.calmlauncher.domain.model.AppLimitDecision.Allowed -> Unit
            }
            val decision = resolveLaunchDecision(request)
            when {
                decision.isBlocked -> _effects.emit(
                    LaunchEffect.Blocked(decision.blockReason ?: "Blocked"),
                )
                decision.isDeadEnd -> _effects.emit(LaunchEffect.DeadEnd(request))
                decision.steps.isEmpty() -> proceed(request)
                else -> _flow.value = LaunchFlowState(request, decision.steps, 0)
            }
        }
    }

    fun grantAppLimitOverrideAndLaunch(request: AppLaunchRequest, minutes: Int) {
        scope.launch {
            appLimitRepository.extendOverride(request.packageName, minutes)
            request(request)
        }
    }

    /** Called by the gate host when the current step has been satisfied. */
    fun onStepComplete() {
        val state = _flow.value ?: return
        if (state.index >= state.steps.lastIndex) {
            proceed(state.request)
        } else {
            _flow.value = state.copy(index = state.index + 1)
        }
    }

    /** Satisfy a [FrictionStep.Reason] step, attaching the user's stated intent. */
    fun submitReason(reason: String) {
        val state = _flow.value ?: return
        _flow.value = state.copy(request = state.request.copy(reason = reason))
        onStepComplete()
    }

    /** Abandon the current launch (user backed out of the friction). */
    fun cancel() {
        _flow.value = null
    }

    private fun proceed(request: AppLaunchRequest) {
        _flow.value = null
        scope.launch {
            recordLaunch(
                LaunchEvent(
                    packageName = request.packageName,
                    category = request.category,
                    timestampEpochMs = System.currentTimeMillis(),
                    reason = request.reason,
                    source = request.source,
                ),
            )
            val ok = appLauncher.launch(request.packageName)
            if (ok) _effects.emit(LaunchEffect.Launched(request.packageName))
        }
    }
}
