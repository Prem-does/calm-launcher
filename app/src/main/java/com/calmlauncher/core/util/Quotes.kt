package com.calmlauncher.core.util

/**
 * Calming, presence-oriented quotes shown in Focus Mode and the dead-end reset screen.
 * Deterministically selected by day so the message is stable within a session.
 */
object Quotes {

    val focus = listOf(
        "\"Presence is the greatest gift.\"",
        "\"Almost everything will work again if you unplug it for a few minutes.\"",
        "\"The present moment is the only place where life exists.\"",
        "\"Do less, but better.\"",
        "\"Your attention is your life.\"",
        "\"Silence is a source of great strength.\"",
        "\"Nature does not hurry, yet everything is accomplished.\"",
        "\"What you stay focused on will grow.\"",
    )

    val reset = listOf(
        "Take a slow breath. There is nothing you need to chase right now.",
        "This feed has no end. You do.",
        "Notice your hands. Notice the room. You are here.",
        "The urge will pass whether you scroll or not.",
        "Rest is not wasted time.",
    )

    fun focusForDay(dayIndex: Int): String = focus[(dayIndex % focus.size).coerceAtLeast(0)]

    fun resetForIndex(index: Int): String = reset[(index % reset.size).coerceAtLeast(0)]
}
