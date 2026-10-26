package app.cybertracker.ui.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cybertracker.CyberApp
import app.cybertracker.data.entity.CustomReminderEntity
import app.cybertracker.data.repository.CustomReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class RemindersState(
    val loading: Boolean = true,
    val upcoming: List<CustomReminderEntity> = emptyList(),
    val past: List<CustomReminderEntity> = emptyList(),
)

fun CustomReminderEntity.at(): LocalDateTime = LocalDateTime.of(date, time)

class RemindersViewModel(repo: CustomReminderRepository) : ViewModel() {
    private val now = MutableStateFlow(LocalDateTime.now())

    val state: StateFlow<RemindersState> = combine(repo.observeAll(), now) { all, n ->
        val (past, upcoming) = all.partition { it.at().isBefore(n) }
        RemindersState(
            loading = false,
            upcoming = upcoming.sortedWith(compareBy({ it.at() }, { it.id })),
            past = past.sortedWith(compareByDescending<CustomReminderEntity> { it.at() }.thenBy { it.id }),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersState())

    /** Called on resume so a reminder that just fired moves to "past". */
    fun refreshNow() {
        now.value = LocalDateTime.now()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CyberApp
                RemindersViewModel(app.container.reminders)
            }
        }
    }
}

const val MAX_REMINDER_TITLE = 100
const val MAX_REMINDER_NOTE = 500

data class ReminderEditorState(
    val loading: Boolean = false,
    val id: String,
    val isNew: Boolean = true,
    val createdAt: Long = 0L,
    val title: String = "",
    val note: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val titleError: Boolean = false,
    val saving: Boolean = false,
    val missing: Boolean = false,
    val error: String? = null,
) {
    val isInPast: Boolean get() = LocalDateTime.of(date, time).isBefore(LocalDateTime.now())
}

class ReminderEditorViewModel(
    private val repo: CustomReminderRepository,
    handle: SavedStateHandle,
) : ViewModel() {
    private val editingId: String? = handle.get<String>("reminderId")

    private val _state = MutableStateFlow(
        run {
            val suggested = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS)
            ReminderEditorState(
                loading = editingId != null,
                id = editingId ?: UUID.randomUUID().toString(),
                isNew = editingId == null,
                date = suggested.toLocalDate(),
                time = suggested.toLocalTime(),
            )
        },
    )
    val state: StateFlow<ReminderEditorState> = _state

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val r = repo.get(editingId)
                _state.value = if (r == null) {
                    _state.value.copy(loading = false, missing = true)
                } else {
                    _state.value.copy(
                        loading = false,
                        createdAt = r.createdAt,
                        title = r.title,
                        note = r.note.orEmpty(),
                        date = r.date,
                        time = r.time,
                    )
                }
            }
        }
    }

    fun onTitle(v: String) {
        _state.value = _state.value.copy(title = v.take(MAX_REMINDER_TITLE), titleError = false, error = null)
    }

    fun onNote(v: String) {
        _state.value = _state.value.copy(note = v.take(MAX_REMINDER_NOTE))
    }

    fun onDate(v: LocalDate) {
        _state.value = _state.value.copy(date = v)
    }

    fun onTime(v: LocalTime) {
        _state.value = _state.value.copy(time = v)
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving || s.loading) return
        val title = s.title.trim()
        if (title.isEmpty()) {
            _state.value = s.copy(titleError = true)
            return
        }
        _state.value = s.copy(saving = true)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            try {
                repo.save(
                    CustomReminderEntity(
                        id = s.id,
                        title = title,
                        note = s.note.trim().ifEmpty { null },
                        date = s.date,
                        time = s.time.withSecond(0).withNano(0),
                        createdAt = if (s.createdAt == 0L) now else s.createdAt,
                        updatedAt = now,
                    ),
                )
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = "Could not save. Please try again.")
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(_state.value.id)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CyberApp
                ReminderEditorViewModel(app.container.reminders, createSavedStateHandle())
            }
        }
    }
}
