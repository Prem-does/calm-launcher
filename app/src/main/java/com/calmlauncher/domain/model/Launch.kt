package com.calmlauncher.domain.model

/** Where a launch was initiated from (affects friction & analytics). */
enum class LaunchSource { HOME, APP_LIST, SEARCH, SHORTCUT }

/** A request to open an app, before any policy is applied. */
data class AppLaunchRequest(
    val packageName: String,
    val label: String,
    val category: AppCategory,
    val source: LaunchSource,
    val reason: String? = null,
)

/**
 * A single friction barrier the user must pass before an app opens. The launch gate
 * presents these in order; [Block]/[DeadEnd] short-circuit the flow.
 */
sealed interface FrictionStep {
    /** Ask "Why are you opening this?" and record the answer. */
    data object Reason : FrictionStep

    /** A countdown the user must wait out. */
    data class Delay(val seconds: Int) : FrictionStep

    /** A yes/no confirmation with a calming [message]. */
    data class Confirm(val message: String) : FrictionStep

    /** Route to the black reset screen instead of opening. */
    data object DeadEnd : FrictionStep

    /** Refuse entirely (e.g. blocked by focus rules), explaining [reason]. */
    data class Block(val reason: String) : FrictionStep
}

/**
 * The ordered set of barriers produced by the ModeEngine for a launch request.
 * An empty list means "open immediately".
 */
data class LaunchDecision(val steps: List<FrictionStep>) {
    val isBlocked: Boolean get() = steps.any { it is FrictionStep.Block }
    val isDeadEnd: Boolean get() = steps.any { it is FrictionStep.DeadEnd }
    val blockReason: String?
        get() = (steps.firstOrNull { it is FrictionStep.Block } as? FrictionStep.Block)?.reason

    companion object {
        val Allow = LaunchDecision(emptyList())
    }
}
