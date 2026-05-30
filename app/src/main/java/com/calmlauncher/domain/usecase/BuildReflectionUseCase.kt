package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.model.ReflectionEntry
import com.calmlauncher.domain.repository.LaunchEventRepository
import com.calmlauncher.domain.repository.ReflectionRepository
import javax.inject.Inject

/**
 * Provides the nightly **Usage Reflection** prompt for a given day.
 *
 * If a reflection already exists for [dayStartEpochMs] (the user may have started writing one),
 * it is returned untouched. Otherwise a fresh, un-persisted [ReflectionEntry] is created with a
 * deterministic prompt chosen for that day. The prompt leans gentler on light-usage days and a
 * touch more probing on heavy ones — always non-judgemental.
 *
 * The returned entry is *not* persisted here; the caller upserts it once the user responds.
 */
class BuildReflectionUseCase @Inject constructor(
    private val reflectionRepository: ReflectionRepository,
    private val launchEventRepository: LaunchEventRepository,
) {
    suspend operator fun invoke(dayStartEpochMs: Long): ReflectionEntry {
        reflectionRepository.latestFor(dayStartEpochMs)?.let { return it }

        val dayEnd = dayStartEpochMs + DAY_MS
        val dayEvents = launchEventRepository.since(dayStartEpochMs)
            .filter { it.timestampEpochMs in dayStartEpochMs until dayEnd }
        val distractingOpens = dayEvents.count { it.category.isDistractingByDefault }

        val prompt = choosePrompt(dayStartEpochMs, distractingOpens)
        return ReflectionEntry(
            dayStartEpochMs = dayStartEpochMs,
            prompt = prompt,
            response = null,
            createdAtEpochMs = dayStartEpochMs,
        )
    }

    private fun choosePrompt(dayStartEpochMs: Long, distractingOpens: Int): String {
        val pool = if (distractingOpens >= HEAVY_USE_THRESHOLD) HEAVY_USE_PROMPTS else GENTLE_PROMPTS
        // Deterministic rotation by day so the prompt is stable for a given date.
        val dayIndex = (dayStartEpochMs / DAY_MS).toInt()
        val idx = ((dayIndex % pool.size) + pool.size) % pool.size
        return pool[idx]
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val HEAVY_USE_THRESHOLD = 25 // distracting opens in a day

        val GENTLE_PROMPTS = listOf(
            "What did today's phone use give you?",
            "What is one moment today you were fully present for?",
            "What did you make time for away from the screen today?",
            "How do you feel about how you spent your attention today?",
        )

        val HEAVY_USE_PROMPTS = listOf(
            "What were you looking for on your phone today?",
            "Which app pulled you back the most, and what for?",
            "If today's screen time were time back, what would you spend it on?",
            "What would you like tomorrow's relationship with your phone to feel like?",
        )
    }
}
