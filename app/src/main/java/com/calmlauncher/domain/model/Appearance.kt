package com.calmlauncher.domain.model

/**
 * Everything under Settings → Customization.
 *
 * **This model is deliberately inert.** Every field here changes how the launcher *looks* and
 * nothing else. No value in this file is read by the ModeEngine, the app-limit system, the reminder
 * scheduler, the blocking services, or any friction rule — and that separation is the whole reason
 * it is a distinct type rather than more fields on [LauncherSettings], which mixes appearance with
 * behaviour and is read by all of those.
 *
 * The rule for anything added here: if changing it could alter what the launcher *does* — how long
 * a delay lasts, which apps are hidden from the list, when something is blocked, how a gesture
 * behaves, what gets logged — it does not belong in this file. Animation speed, gesture behaviour,
 * performance and battery options, blocking, reminders and timers were all explicitly excluded for
 * that reason: a "make it look nicer" screen must never become a place to quietly loosen a limit.
 *
 * Note the one genuine boundary case, [density]. It changes spacing and padding only. It does *not*
 * change how many apps are eligible to be shown, or which ones — [gridColumns] likewise governs
 * layout, not filtering.
 */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.NONE,
    val fontStyle: FontStyle = FontStyle.SANS,
    val fontScale: FontScale = FontScale.NORMAL,
    val gridColumns: HomeGridColumns = HomeGridColumns.ONE,
    val clockStyle: ClockStyle = ClockStyle.LARGE,
    val searchBarStyle: SearchBarStyle = SearchBarStyle.UNDERLINE,
    val density: LayoutDensity = LayoutDensity.COMFORTABLE,
)

/**
 * Light, dark, or follow the device.
 *
 * [SYSTEM] is the default because it is the only option that stays correct on its own — a user who
 * has set a device-wide schedule has already answered this question once.
 */
enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
}

/**
 * A restrained accent, applied only to selection states, toggles, focus rings and active
 * indicators.
 *
 * Body text, backgrounds and app names stay monochrome regardless of what is chosen here. That
 * limit is on purpose: the launcher's whole premise is a low-stimulation surface, and a colour
 * applied broadly across it would undo the thing people install it for. The palette is muted for
 * the same reason — these are desaturated at roughly 45–55% saturation rather than being primaries.
 *
 * [NONE] keeps the launcher fully monochrome, and remains the default.
 */
enum class AccentColor(val label: String, val lightArgb: Long, val darkArgb: Long) {
    NONE("None", 0xFF111111, 0xFFFFFFFF),
    SAGE("Sage", 0xFF4F6B54, 0xFF9DBBA3),
    CLAY("Clay", 0xFF8A5A46, 0xFFC79680),
    SLATE("Slate", 0xFF4A5D73, 0xFF9BB0C7),
    PLUM("Plum", 0xFF6B4F63, 0xFFBFA0B6),
    MOSS("Moss", 0xFF5E6B3F, 0xFFB4C089),
    ;

    val isMonochrome: Boolean get() = this == NONE
}

/** Typeface for the whole launcher. */
enum class FontStyle(val label: String) {
    /** The launcher's own IBM Plex Sans. */
    PLEX("Plex Sans"),

    /** The platform sans stack — matches the rest of the device. */
    SANS("System Sans"),

    /** Platform serif, for a quieter, more editorial feel. */
    SERIF("Serif"),

    /** Platform monospace. */
    MONO("Monospace"),
}

/**
 * Text size, as a multiplier on the launcher's type ramp.
 *
 * This is *in addition to* the device's own font-size setting, not a replacement for it: the
 * platform scale is still honoured, so someone who has enlarged text system-wide does not have it
 * silently overridden here.
 */
enum class FontScale(val label: String, val multiplier: Float) {
    SMALL("Small", 0.88f),
    NORMAL("Normal", 1.0f),
    LARGE("Large", 1.14f),
    LARGEST("Largest", 1.28f),
}

/** How many columns the home favourites use. */
enum class HomeGridColumns(val label: String, val columns: Int) {
    /** The default single-column list of names. */
    ONE("List", 1),
    TWO("Two columns", 2),
    THREE("Three columns", 3),
    FOUR("Four columns", 4),
}

/** How the home clock is presented. */
enum class ClockStyle(val label: String) {
    /** The oversized hero clock. */
    LARGE("Large"),

    /** A smaller, single-line time. */
    COMPACT("Compact"),

    /** Time plus the full date. */
    WITH_DATE("Time and date"),

    /** No clock at all. */
    HIDDEN("Hidden"),
}

/**
 * How the search field is drawn.
 *
 * [UNDERLINE] is first and is the default because it is the launcher's existing treatment — anyone
 * who never opens Customization must see exactly what they saw before. There is deliberately no
 * "hidden" option: removing the input from the search screen would stop the screen doing its job,
 * which is a behavioural change and therefore out of scope for this section.
 */
enum class SearchBarStyle(val label: String) {
    /** A single rule beneath the text. The launcher's original look. */
    UNDERLINE("Underline"),

    /** A rounded outlined container. */
    OUTLINED("Outlined"),

    /** A filled surface container. */
    FILLED("Filled"),

    /** No border, rule or fill — just the caret and the text. */
    MINIMAL("Minimal"),
}

/**
 * Spacing and padding throughout the launcher, as a multiplier on the 8dp base scale.
 *
 * Affects whitespace only. It does not change how many items are rendered or which — a denser
 * layout shows the same apps closer together, and never a different set.
 */
enum class LayoutDensity(val label: String, val multiplier: Float) {
    COMPACT("Compact", 0.72f),
    COMFORTABLE("Comfortable", 1.0f),
    SPACIOUS("Spacious", 1.3f),
}
