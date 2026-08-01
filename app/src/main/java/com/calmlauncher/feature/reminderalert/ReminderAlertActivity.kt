package com.calmlauncher.feature.reminderalert

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.theme.CalmTheme
import com.calmlauncher.domain.model.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

/**
 * The full-screen reminder interruption.
 *
 * This replaces the reminder *notification* as the primary surface, for the reason the user gave:
 * a notification is a suggestion, and a reminder someone deliberately set for themselves should
 * not be dismissible by a swipe they make without reading it. So this is an Activity that covers
 * whatever they were doing, dims it, and offers exactly two ways forward.
 *
 * Why an Activity rather than only a window overlay:
 *  - it can be brought up over the lock screen ([setShowWhenLocked]) and can turn the screen on,
 *    which a `TYPE_APPLICATION_OVERLAY` window cannot;
 *  - it needs no optional permission, so it works on a fresh install before the user has granted
 *    "display over other apps";
 *  - it gets a real Compose tree, which is what makes the launcher's own typography usable here.
 *
 * It is launched from a full-screen-intent notification (the only reliable way for a background
 * broadcast to take the screen on Android 10+) with a direct `startActivity` as a second attempt.
 * [com.calmlauncher.notification.ReminderAlertPresenter] owns that decision, and falls back to a
 * window overlay and finally a plain notification when neither route is open — a reminder is
 * never silently dropped just because it couldn't be shown the best way.
 *
 * `singleInstance` + [onNewIntent]: a second alarm arriving while this is up rebinds the existing
 * screen instead of stacking another copy, so the user can never be left with two overlays to
 * clear for the same reminder.
 */
@AndroidEntryPoint
class ReminderAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        dimAndBlurBehind()

        setContent {
            val viewModel: ReminderAlertViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

            // Bound from composition rather than onCreate so a configuration change re-binds
            // against the retained ViewModel, where it no-ops. Rotating must not restart the flow
            // or reset a half-made snooze choice.
            LaunchedEffect(reminderId) { viewModel.bind(reminderId) }

            CalmTheme(themePreference = ThemePreference.DARK) {
                // Back is swallowed on purpose. The overlay is an interruption the user has to
                // answer; Snooze and Finished are the only exits. Home and recents still belong
                // to the system, which is correct — this is a reminder, not a lock.
                BackHandler(enabled = true) { /* deliberately inert */ }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = SCRIM_ALPHA)),
                ) {
                    when (val current = state) {
                        is ReminderAlertUiState.Showing -> ReminderAlertScreen(
                            reminder = current.reminder,
                            snoozePickerOpen = current.snoozePickerOpen,
                            busy = current.busy,
                            onSnoozeClick = viewModel::openSnoozePicker,
                            onSnoozeChosen = viewModel::snooze,
                            onSnoozeDismiss = viewModel::closeSnoozePicker,
                            onFinished = viewModel::finish,
                        )

                        ReminderAlertUiState.Loading -> Unit

                        // The ViewModel only reaches Dismissed once its write has completed, so
                        // finishing here can't lose a "Finished" tap.
                        ReminderAlertUiState.Dismissed ->
                            LaunchedEffect(Unit) { finishAndRemoveTask() }
                    }
                }
            }
        }
    }

    /**
     * A second reminder arriving while this one is still up. Replacing the intent (rather than
     * launching another instance) is what keeps "one overlay at a time" true. The ViewModel
     * survives [recreate], and rebinding to a different id is what moves the screen on.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        // Ask the system to take down a non-secure keyguard so the actions are reachable. On a
        // secured device this is a no-op and the user unlocks first, which is the right trade —
        // a reminder is not a reason to get behind a lock screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                keyguard?.requestDismissKeyguard(this, null)
            }
        }
    }

    /**
     * Dim, and on Android 12+ blur, whatever is behind. This is the visual half of "prevent
     * interaction with the current app": the app stays visible but plainly out of reach, rather
     * than being replaced by an opaque screen that reads as a crash.
     *
     * `FLAG_BLUR_BEHIND` is a request, not a guarantee — the platform ignores it when blurs are
     * disabled for battery or accessibility reasons, which is why the dim carries the effect on
     * its own and the blur is only ever a bonus.
     */
    private fun dimAndBlurBehind() {
        runCatching {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply {
                dimAmount = BEHIND_DIM
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    blurBehindRadius = BLUR_RADIUS_PX
                }
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminderId"

        /** How much of the underlying app shows through. Present, but plainly unreachable. */
        private const val BEHIND_DIM = 0.85f
        private const val SCRIM_ALPHA = 0.82f
        private const val BLUR_RADIUS_PX = 48

        /**
         * An intent that shows [reminderId]. `NEW_TASK` + `CLEAR_TASK` keeps the overlay out of
         * the launcher's own task, so dismissing it can never leave the user staring at a
         * half-built launcher back stack.
         */
        fun intent(context: Context, reminderId: Long): Intent =
            Intent(context, ReminderAlertActivity::class.java)
                .putExtra(EXTRA_REMINDER_ID, reminderId)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION,
                )
    }
}
