package com.calmlauncher.feature.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.ReflectionEntry
import com.calmlauncher.domain.repository.ReflectionRepository
import com.calmlauncher.domain.repository.ScreenTimeRepository
import com.calmlauncher.domain.usecase.BuildInsightsUseCase
import com.calmlauncher.domain.usecase.BuildReflectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Presentational state for the **Usage Reflection** screen. [prompt] is tonight's gentle,
 * deterministic question; [response] mirrors what the user has typed so far; [insights] are the
 * week's neutral [com.calmlauncher.domain.model.Insight] lines from the Calm AI Assistant; and
 * [screenTimeText] is today's pre-formatted foreground total.
 */
data class ReflectionUiState(
    val prompt: String = "",
    val response: String = "",
    val insights: List<String> = emptyList(),
    val screenTimeText: String = "",
    val recentNotes: List<ReflectionNoteUi> = emptyList(),
    val saveStatusText: String = "",
)

data class ReflectionNoteUi(
    val dayStartEpochMs: Long,
    val dayLabel: String,
    val prompt: String,
    val response: String,
)

/**
 * Drives the nightly **Usage Reflection** + **Calm AI Assistant** surface.
 *
 * On init it resolves today's day-start (local midnight) and, through [BuildReflectionUseCase],
 * ensures a [ReflectionEntry] exists for today — reusing an in-progress one if the user already
 * started writing, otherwise materialising a fresh (un-persisted) prompt. That entry is held in
 * [entry] so [onResponseChange] can edit a local copy and [save] can insert it without
 * re-deriving the prompt.
 *
 * The visible [uiState] combines the held entry, the week's insights and today's screen time into
 * a single immutable snapshot, deliberately phrased to observe rather than judge.
 */
@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val reflectionRepository: ReflectionRepository,
    screenTimeRepository: ScreenTimeRepository,
    buildInsights: BuildInsightsUseCase,
    private val buildReflection: BuildReflectionUseCase,
) : ViewModel() {

    // Local midnight for today, in epoch millis — the key for today's reflection entry.
    private val dayStart: Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    /** Today's reflection entry (prompt + any saved/in-progress response). */
    private val entry = MutableStateFlow<ReflectionEntry?>(null)
    private val draftResponse = MutableStateFlow("")
    private val lastSavedAt = MutableStateFlow<Long?>(null)
    private val recentNotesAndSave = combine(
        reflectionRepository.observeRecent(NOTE_HISTORY_LIMIT),
        lastSavedAt,
    ) { recent, savedAt -> recent to savedAt }

    val uiState: StateFlow<ReflectionUiState> = combine(
        entry,
        draftResponse,
        buildInsights(),
        screenTimeRepository.observeToday(),
        recentNotesAndSave,
    ) { current, draft, insights, screenTime, notesAndSave ->
        val (recent, savedAt) = notesAndSave
        ReflectionUiState(
            prompt = current?.prompt.orEmpty(),
            response = draft,
            insights = insights.map { it.text },
            screenTimeText = screenTime.format(),
            recentNotes = recent
                .filter { !it.response.isNullOrBlank() }
                .map { it.toNoteUi() },
            saveStatusText = if (savedAt != null) "Saved" else "",
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ReflectionUiState(),
    )

    init {
        viewModelScope.launch {
            val today = buildReflection(dayStart)
            entry.value = today
        }
    }

    /** Mirror the user's edits into the held entry without persisting them. */
    fun onResponseChange(s: String) {
        draftResponse.value = s
        entry.update { it?.copy(response = s, createdAtEpochMs = System.currentTimeMillis()) }
    }

    /** Persist today's reflection, prompt and current response together. */
    fun save() {
        viewModelScope.launch {
            persist(draftResponse.value)
        }
    }

    private suspend fun persist(response: String) {
        if (response.isBlank()) return
        val current = entry.value ?: buildReflection(dayStart).also { entry.value = it }
        val updated = current.copy(
            id = 0L,
            response = response,
            createdAtEpochMs = System.currentTimeMillis(),
        )
        reflectionRepository.insert(updated)
        draftResponse.value = ""
        entry.update { it?.copy(response = null) }
        lastSavedAt.value = updated.createdAtEpochMs
    }

    private fun ReflectionEntry.toNoteUi(): ReflectionNoteUi =
        ReflectionNoteUi(
            dayStartEpochMs = dayStartEpochMs,
            dayLabel = dayStartEpochMs.toDayLabel(),
            prompt = prompt,
            response = response.orEmpty(),
        )

    private fun Long.toDayLabel(): String =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .format(NoteDateFormatter)

    private companion object {
        const val NOTE_HISTORY_LIMIT = 30

        val NoteDateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    }
}
