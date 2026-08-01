package com.calmlauncher.feature.gate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.launcher.LaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin presentation seam over the central [LaunchCoordinator]. The gate host owns no
 * launch logic of its own; it merely surfaces the coordinator's [flow] (the currently
 * displayed friction sequence) and one-shot [effects], and forwards user gestures back
 * to the state machine. All launching, recording and friction sequencing lives in the
 * coordinator so it survives navigation and process-level scope.
 */
@HiltViewModel
class GateViewModel @Inject constructor(
    val coordinator: LaunchCoordinator,
    private val appLimitRepository: AppLimitRepository,
) : ViewModel() {

    /**
     * How many minutes one extension is worth, per the user's configuration. Read once and held so
     * the block screen can name the real figure instead of a hard-coded "10 minutes" that may not
     * match what an extension actually buys.
     */
    var minutesPerExtension by mutableIntStateOf(DEFAULT_EXTENSION_MINUTES)
        private set

    init {
        viewModelScope.launch {
            minutesPerExtension = runCatching { appLimitRepository.minutesPerExtension() }
                .getOrDefault(DEFAULT_EXTENSION_MINUTES)
        }
    }

    /** The current friction step to render, or null when no gate is showing. */
    val flow = coordinator.flow

    /** One-shot effects (navigate to reset, blocked message, launched). */
    val effects = coordinator.effects

    /** The current step has been satisfied — advance the sequence. */
    fun stepComplete() = coordinator.onStepComplete()

    /** Satisfy a Reason step with the user's stated intent. */
    fun submitReason(r: String) = coordinator.submitReason(r)

    /** Abandon the current launch (user backed out of the friction). */
    fun cancel() = coordinator.cancel()

    private companion object {
        const val DEFAULT_EXTENSION_MINUTES = 10
    }
}
