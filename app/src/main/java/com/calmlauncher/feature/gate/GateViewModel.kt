package com.calmlauncher.feature.gate

import androidx.lifecycle.ViewModel
import com.calmlauncher.launcher.LaunchCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

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
}
