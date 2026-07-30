package com.calmlauncher.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.calmlauncher.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** What the reminder overlay should say. */
data class ReminderOverlaySpec(
    val reminderId: Long,
    val title: String,
    val note: String,
    val dueLabel: String?,
    val snoozeChoices: List<Int>,
)

/**
 * The fallback reminder interruption: a `TYPE_APPLICATION_OVERLAY` window drawn straight over
 * whatever app is in front.
 *
 * [com.calmlauncher.feature.reminderalert.ReminderAlertActivity] is the preferred surface and
 * this exists for the cases it can't cover. Two of them are real and common:
 *
 *  - **Android 14+ withholds `USE_FULL_SCREEN_INTENT`** from apps that aren't calling or alarm
 *    apps unless the user grants it. Without that grant a full-screen intent degrades to an
 *    ordinary heads-up notification, which is exactly the dismissible thing we were asked to
 *    replace. An overlay window has no such restriction once "display over other apps" is on.
 *  - **Some OEM builds lose the background-activity-launch race** and drop the Activity behind
 *    the foreground app. A window overlay is composited above it by definition.
 *
 * Raw Views rather than Compose on purpose: a `ComposeView` in a window-manager window needs a
 * lifecycle and saved-state owner grafted onto it by hand, and getting that subtly wrong crashes
 * inside an alarm broadcast — the worst possible place. The styling here deliberately mirrors
 * [BlockOverlayController] so the two interruptions feel like the same launcher.
 */
@Singleton
class ReminderOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val main = Handler(Looper.getMainLooper())
    private val windowManager: WindowManager?
        get() = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var root: FrameLayout? = null
    private var showingReminderId: Long? = null

    /** True when the user has granted "display over other apps". */
    fun canShow(): Boolean = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

    /** The reminder currently on screen, or null when nothing is showing. */
    fun showingReminderId(): Long? = showingReminderId

    /**
     * Cover the screen with [spec]. Returns false when the overlay could not be attached, which
     * is the caller's cue to fall back further rather than assume the user has been told.
     *
     * Re-showing the same reminder is a no-op, so a duplicate alarm delivery can't stack two
     * windows for one reminder.
     */
    fun show(
        spec: ReminderOverlaySpec,
        onSnooze: (minutes: Int) -> Unit,
        onFinished: () -> Unit,
    ): Boolean {
        if (!canShow()) return false
        if (showingReminderId == spec.reminderId && root != null) return true

        var attached = false
        runOnMain {
            removeInternal()
            attached = attachInternal(spec, onSnooze, onFinished)
        }
        // Off the main thread the post hasn't run yet; assume it will, because a false negative
        // here would make the caller post a duplicate notification for a reminder we did show.
        return if (Looper.myLooper() == Looper.getMainLooper()) attached else true
    }

    /** Take the overlay down. Safe from any thread, and safe when nothing is showing. */
    fun hide() = runOnMain { removeInternal() }

    // -----------------------------------------------------------------------------------
    // Internals — everything below runs on the main thread.
    // -----------------------------------------------------------------------------------

    private fun attachInternal(
        spec: ReminderOverlaySpec,
        onSnooze: (Int) -> Unit,
        onFinished: () -> Unit,
    ): Boolean {
        val wm = windowManager ?: return false
        val view = buildView(spec, onSnooze, onFinished)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Focusable on purpose: without input focus the back key never reaches us and the
            // interruption could be backed out of without answering it.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        return runCatching {
            wm.addView(view, params)
            root = view
            showingReminderId = spec.reminderId
            true
        }.getOrElse {
            // Permission revoked between the check and the add, or no window token.
            root = null
            showingReminderId = null
            false
        }
    }

    private fun removeInternal() {
        val view = root ?: run {
            showingReminderId = null
            return
        }
        runCatching { windowManager?.removeView(view) }
        root = null
        showingReminderId = null
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun buildView(
        spec: ReminderOverlaySpec,
        onSnooze: (Int) -> Unit,
        onFinished: () -> Unit,
    ): FrameLayout {
        val font = runCatching { ResourcesCompat.getFont(context, R.font.ibm_plex_sans) }
            .getOrNull()

        // Fires at most once, whichever action the user takes.
        var answered = false
        val answer: (() -> Unit) -> Unit = { action ->
            if (!answered) {
                answered = true
                removeInternal()
                action()
            }
        }

        val container = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                // Swallow back: the reminder has to be answered, not dismissed.
                if (event.keyCode == KeyEvent.KEYCODE_BACK) return true
                return super.dispatchKeyEvent(event)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean = true
        }.apply {
            setBackgroundColor(SCRIM)
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), dp(72), dp(32), dp(72))
            layoutParams = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER)
        }

        column.addView(
            label("REMINDER", 12f, MUTED, font).apply { letterSpacing = 0.18f },
        )
        column.addView(label(spec.title, 30f, FOREGROUND, font).withTopMargin(dp(16)))
        spec.dueLabel?.let {
            column.addView(label(it, 15f, MUTED, font).withTopMargin(dp(10)))
        }
        if (spec.note.isNotBlank()) {
            column.addView(label(spec.note, 15f, MUTED, font).withTopMargin(dp(16)))
        }

        // Snooze durations start hidden and replace the two main actions in place, so choosing
        // one is a single tap after the decision to snooze — same shape as the Compose version.
        val snoozeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(40) }
        }
        spec.snoozeChoices.forEach { minutes ->
            snoozeRow.addView(
                actionButton("${minutes}m", font, MUTED) { answer { onSnooze(minutes) } }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                        marginStart = dp(4)
                        marginEnd = dp(4)
                    }
                    setPadding(dp(4), dp(12), dp(4), dp(12))
                },
            )
        }

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(40) }
        }
        actions.addView(
            actionButton("Snooze", font, FOREGROUND) {
                actions.visibility = View.GONE
                snoozeRow.visibility = View.VISIBLE
            },
        )
        actions.addView(
            actionButton("Finished", font, FOREGROUND) { answer(onFinished) }
                .withTopMargin(dp(12)),
        )

        column.addView(actions)
        column.addView(snoozeRow)
        container.addView(column)
        return container
    }

    // -----------------------------------------------------------------------------------
    // Small view helpers — kept local so the overlay has no layout XML to keep in sync.
    // -----------------------------------------------------------------------------------

    private fun label(
        text: String,
        sizeSp: Float,
        color: Int,
        font: Typeface?,
    ): TextView = TextView(context).apply {
        this.text = text
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        gravity = Gravity.CENTER
        font?.let { setTypeface(it) }
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
    }

    private fun actionButton(
        text: String,
        font: Typeface?,
        strokeColor: Int,
        onClick: () -> Unit,
    ): Button = Button(context).apply {
        this.text = text
        setTextColor(FOREGROUND)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        isAllCaps = false
        font?.let { setTypeface(it) }
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(28).toFloat()
            setColor(Color.TRANSPARENT)
            setStroke(dp(1).coerceAtLeast(1), strokeColor)
        }
        setPadding(dp(24), dp(14), dp(24), dp(14))
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        setOnClickListener { onClick() }
    }

    private fun <T : View> T.withTopMargin(px: Int): T = apply {
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = px }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    private companion object {
        val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

        /** Near-opaque rather than pure black, so the app behind stays faintly visible. */
        val SCRIM = Color.argb(0xEB, 0x00, 0x00, 0x00)
        val FOREGROUND = Color.WHITE
        val MUTED = Color.rgb(0xB3, 0xB3, 0xB3)
    }
}
