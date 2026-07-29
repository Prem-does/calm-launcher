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

/**
 * What a block overlay should say and how long it should hold the screen.
 *
 * @param packageName the app being covered — also the identity used to avoid re-showing the
 *   same overlay on every window event the app emits.
 * @param title the headline, e.g. "Limit reached".
 * @param appLabel the app the user is currently in.
 * @param detail one line of context: usage against the limit, or the reason for the block.
 * @param countdownSeconds how long the user sits with the message before anything is tappable.
 * @param overrideLabel label for the escape hatch, or null when none is available.
 * @param graceSeconds how long the actions stay up before the user is sent home anyway. Zero
 *   means the exit is immediate once the countdown ends.
 * @param footnote small print under the actions, e.g. "Both extensions are used up today".
 */
data class BlockOverlaySpec(
    val packageName: String,
    val title: String,
    val appLabel: String,
    val detail: String,
    val countdownSeconds: Int,
    val overrideLabel: String? = null,
    val graceSeconds: Int = 0,
    val footnote: String? = null,
)

/**
 * Draws a full-screen black overlay **on top of another app** and, when it has had its say,
 * hands control back to the caller to push the user out.
 *
 * Why this exists: the in-launcher [com.calmlauncher.feature.gate.BlockCountdownOverlay] only
 * ever sees launches that start inside Calm. An app opened from a notification, from recents,
 * or from another app used to be dealt with by an instant, unexplained bounce to the home
 * screen — the user was ejected mid-scroll with no idea why. This covers that same moment with
 * the same message the launch gate would have shown, sits there for a beat, and *then* exits.
 *
 * Two deliberate constraints:
 *  - **It cannot be dismissed by tapping.** The root view swallows touches and the back key,
 *    because the pause is the feature. The only ways out are the offered actions and the
 *    automatic exit, both of which are on timers the user can watch tick down.
 *  - **It never leaves the user stranded.** Every path ends in `onExit`, and [hide] is safe to
 *    call from anywhere, so a lost callback or a revoked permission can't trap someone behind
 *    an undismissable black screen.
 *
 * Requires the "display over other apps" grant (SYSTEM_ALERT_WINDOW). [canShow] reports
 * whether that is in place; callers must fall back to the plain bounce when it isn't.
 */
@Singleton
class BlockOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val main = Handler(Looper.getMainLooper())
    private val windowManager: WindowManager?
        get() = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var root: FrameLayout? = null
    private var showingPackage: String? = null
    private var ticker: Runnable? = null

    /** True when the user has granted "display over other apps". */
    fun canShow(): Boolean = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

    /** The package currently covered, or null when no overlay is up. */
    fun showingPackage(): String? = showingPackage

    /**
     * Cover the screen with [spec]. Returns false when the overlay can't be shown (permission
     * missing, or window manager refused), which is the caller's cue to bounce the user home
     * the old way rather than silently letting the app carry on.
     *
     * Showing the same package twice is a no-op, so this is safe to call from an accessibility
     * event handler that fires several times per second.
     */
    fun show(
        spec: BlockOverlaySpec,
        onOverride: (() -> Unit)? = null,
        onExit: () -> Unit,
    ): Boolean {
        if (!canShow()) return false
        if (showingPackage == spec.packageName && root != null) return true

        var attached = false
        runOnMain {
            removeInternal()
            attached = attachInternal(spec, onOverride, onExit)
        }
        // On the main thread the block above has already run; off it, assume the post will
        // succeed — the caller only uses the result to decide on a fallback bounce, and a
        // false negative there would eject the user without the explanation.
        return if (Looper.myLooper() == Looper.getMainLooper()) attached else true
    }

    /** Take the overlay down. Safe from any thread, and safe when nothing is showing. */
    fun hide() = runOnMain { removeInternal() }

    // -----------------------------------------------------------------------------------
    // Internals — everything below runs on the main thread.
    // -----------------------------------------------------------------------------------

    private fun attachInternal(
        spec: BlockOverlaySpec,
        onOverride: (() -> Unit)?,
        onExit: () -> Unit,
    ): Boolean {
        val wm = windowManager ?: return false
        val built = buildView(spec, onOverride, onExit)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Focusable on purpose: without input focus the back key never reaches us and the
            // user could simply back out of the pause.
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
            wm.addView(built.root, params)
            root = built.root
            showingPackage = spec.packageName
            // Only start the clock once the view is actually on screen, so a failed add can't
            // leave a timer running that pushes the user home from nowhere.
            built.start()
            true
        }.getOrElse {
            // Permission revoked between the check and the add, or no window token. Report the
            // failure so the caller falls back instead of leaving the app open and unblocked.
            root = null
            showingPackage = null
            false
        }
    }

    /** A built overlay: the view to attach, and the timer to start once it is attached. */
    private class BuiltOverlay(val root: FrameLayout, val start: () -> Unit)

    private fun removeInternal() {
        ticker?.let { main.removeCallbacks(it) }
        ticker = null
        val view = root ?: run {
            showingPackage = null
            return
        }
        runCatching { windowManager?.removeView(view) }
        root = null
        showingPackage = null
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun buildView(
        spec: BlockOverlaySpec,
        onOverride: (() -> Unit)?,
        onExit: () -> Unit,
    ): BuiltOverlay {
        val font = runCatching { ResourcesCompat.getFont(context, R.font.ibm_plex_sans) }
            .getOrNull()

        // Fired at most once, whichever way the overlay ends.
        var finished = false
        val finish: (Boolean) -> Unit = { pushOut ->
            if (!finished) {
                finished = true
                removeInternal()
                if (pushOut) onExit()
            }
        }

        val container = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                // Swallow back so the pause can't be dismissed. Home and recents belong to the
                // system and are not ours to intercept — that's fine, leaving is the point.
                if (event.keyCode == KeyEvent.KEYCODE_BACK) return true
                return super.dispatchKeyEvent(event)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean = true
        }.apply {
            setBackgroundColor(BACKGROUND)
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            // No layoutParams here on purpose — WindowManager.addView installs its own.
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), dp(72), dp(32), dp(72))
            layoutParams = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER)
        }

        column.addView(
            label(spec.title.uppercase(), 12f, MUTED, font).apply {
                letterSpacing = 0.18f
            },
        )
        column.addView(label(spec.appLabel, 30f, FOREGROUND, font).withTopMargin(dp(16)))
        column.addView(label(spec.detail, 15f, MUTED, font).withTopMargin(dp(10)))

        val counter = label("${spec.countdownSeconds}", 64f, FOREGROUND, font)
            .withTopMargin(dp(40))
        column.addView(counter)

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(40) }
        }
        if (spec.overrideLabel != null && onOverride != null) {
            actions.addView(
                actionButton(spec.overrideLabel, font) {
                    // The user bought more time: leave them where they are.
                    finish(false)
                    onOverride()
                },
            )
        }
        actions.addView(
            actionButton("Close ${spec.appLabel}", font) { finish(true) }
                .withTopMargin(dp(12)),
        )
        spec.footnote?.let {
            actions.addView(label(it, 13f, MUTED, font).withTopMargin(dp(20)))
        }
        column.addView(actions)

        val exitNote = label("", 13f, MUTED, font).withTopMargin(dp(20)).apply {
            visibility = View.GONE
        }
        column.addView(exitNote)

        container.addView(column)

        return BuiltOverlay(container) { startCountdown(spec, counter, actions, exitNote, finish) }
    }

    /**
     * Runs the two-phase timer: the silent countdown, then — if there is anything to offer —
     * a short grace window with the actions visible, narrating the exit as it approaches.
     */
    @SuppressLint("SetTextI18n")
    private fun startCountdown(
        spec: BlockOverlaySpec,
        counter: TextView,
        actions: View,
        exitNote: TextView,
        finish: (Boolean) -> Unit,
    ) {
        var remaining = spec.countdownSeconds
        var grace = spec.graceSeconds
        val hasActions = actions is ViewGroup && actions.childCount > 0

        val tick = object : Runnable {
            override fun run() {
                if (remaining > 0) {
                    counter.text = "$remaining"
                    remaining--
                    main.postDelayed(this, 1_000L)
                    return
                }

                // Countdown done. Nothing to offer, or the grace is spent: out you go.
                if (!hasActions || grace <= 0) {
                    finish(true)
                    return
                }

                if (actions.visibility != View.VISIBLE) {
                    counter.visibility = View.GONE
                    actions.visibility = View.VISIBLE
                    exitNote.visibility = View.VISIBLE
                }
                exitNote.text = if (grace == 1) {
                    "Going home in 1 second"
                } else {
                    "Going home in $grace seconds"
                }
                grace--
                main.postDelayed(this, 1_000L)
            }
        }
        ticker = tick
        main.post(tick)
    }

    // -----------------------------------------------------------------------------------
    // Small view helpers — kept local so the overlay has no layout XML to keep in sync.
    // -----------------------------------------------------------------------------------

    // `font` rather than `typeface`: inside an apply block the receiver's own `typeface`
    // property would shadow a parameter of that name, and the font would silently be set to
    // itself.
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
            setStroke(dp(1).coerceAtLeast(1), FOREGROUND)
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

        /** Pure black on white, matching the launcher's own block screen. */
        val BACKGROUND = Color.BLACK
        val FOREGROUND = Color.WHITE
        val MUTED = Color.rgb(0xB3, 0xB3, 0xB3)
    }
}
