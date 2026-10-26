package app.cybertracker.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cybertracker.CyberApp
import app.cybertracker.data.entity.DateType
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.data.entity.EntryEntity
import app.cybertracker.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

const val MAX_TITLE = 100
const val MAX_LABEL = 60
const val MAX_REMARKS = 2000

data class DateRowState(
    val id: String,
    val label: String = "",
    val date: LocalDate,
    val type: DateType = DateType.ORDINARY,
    val notify: Boolean = true,
    val createdAt: Long = 0L,
    val labelError: Boolean = false,
)

data class EditorState(
    val loading: Boolean = false,
    val entryId: String,
    val isNew: Boolean = true,
    val createdAt: Long = 0L,
    val title: String = "",
    val remarks: String = "",
    val rows: List<DateRowState> = emptyList(),
    val titleError: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
) {
    /** True when saving would schedule at least one notification. */
    val wantsNotifications: Boolean get() = rows.any { it.type == DateType.EXPIRY && it.notify }
}

class EditorViewModel(
    private val repository: EntryRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private val editingId: String? = handle.get<String>("entryId")

    private val _state = MutableStateFlow(
        decodeDraft(handle.get<String>(DRAFT_KEY))
            ?: EditorState(loading = editingId != null, entryId = editingId ?: UUID.randomUUID().toString(), isNew = editingId == null),
    )
    val state: StateFlow<EditorState> = _state

    init {
        // Load from the database only if there is no draft restored after process recreation.
        if (editingId != null && _state.value.loading) {
            viewModelScope.launch {
                val loaded = repository.get(editingId)
                if (loaded == null) {
                    _state.value = _state.value.copy(loading = false, error = "This entry no longer exists.")
                } else {
                    _state.value = EditorState(
                        entryId = loaded.entry.id,
                        isNew = false,
                        createdAt = loaded.entry.createdAt,
                        title = loaded.entry.title,
                        remarks = loaded.entry.remarks.orEmpty(),
                        rows = loaded.dates.sortedWith(compareBy({ it.createdAt }, { it.id })).map {
                            DateRowState(it.id, it.label, it.date, it.type, it.notificationEnabled, it.createdAt)
                        },
                    )
                }
            }
        }
    }

    private fun edit(transform: (EditorState) -> EditorState) {
        val next = transform(_state.value).copy(dirty = true, error = null)
        _state.value = next
        if (!next.loading) handle[DRAFT_KEY] = encodeDraft(next)
    }

    fun onTitle(value: String) = edit { it.copy(title = value.take(MAX_TITLE), titleError = false) }
    fun onRemarks(value: String) = edit { it.copy(remarks = value.take(MAX_REMARKS)) }

    fun addRow() = edit {
        it.copy(rows = it.rows + DateRowState(id = UUID.randomUUID().toString(), date = LocalDate.now()))
    }

    fun removeRow(id: String) = edit { s -> s.copy(rows = s.rows.filterNot { it.id == id }) }

    private fun updateRow(id: String, f: (DateRowState) -> DateRowState) =
        edit { s -> s.copy(rows = s.rows.map { if (it.id == id) f(it) else it }) }

    fun onLabel(id: String, v: String) = updateRow(id) { it.copy(label = v.take(MAX_LABEL), labelError = false) }
    fun onDate(id: String, v: LocalDate) = updateRow(id) { it.copy(date = v) }
    fun onType(id: String, v: DateType) = updateRow(id) { it.copy(type = v) }
    fun onNotify(id: String, v: Boolean) = updateRow(id) { it.copy(notify = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving || s.loading) return
        val title = s.title.trim()
        val rows = s.rows.map { it.copy(labelError = it.label.isBlank()) }
        if (title.isEmpty() || rows.any { it.labelError }) {
            _state.value = s.copy(titleError = title.isEmpty(), rows = rows)
            return
        }
        _state.value = s.copy(saving = true, rows = rows)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entry = EntryEntity(
                id = s.entryId,
                title = title,
                remarks = s.remarks.trim().ifEmpty { null },
                createdAt = if (s.createdAt == 0L) now else s.createdAt,
                updatedAt = now,
            )
            val dates = rows.map {
                EntryDateEntity(
                    id = it.id,
                    entryId = s.entryId,
                    label = it.label.trim(),
                    date = it.date,
                    type = it.type,
                    notificationEnabled = it.type == DateType.EXPIRY && it.notify,
                    createdAt = if (it.createdAt == 0L) now else it.createdAt,
                    updatedAt = now,
                )
            }
            try {
                repository.save(entry, dates)
                handle.remove<String>(DRAFT_KEY)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = "Could not save. Please try again.")
            }
        }
    }

    /** Discards the saved draft (used when the user confirms leaving without saving). */
    fun discardDraft() {
        handle.remove<String>(DRAFT_KEY)
    }

    companion object {
        private const val DRAFT_KEY = "draft"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CyberApp
                EditorViewModel(app.container.repository, createSavedStateHandle())
            }
        }

        // Draft persistence so edits survive process death.
        private fun encodeDraft(s: EditorState): String {
            val o = JSONObject()
            o.put("entryId", s.entryId)
            o.put("isNew", s.isNew)
            o.put("createdAt", s.createdAt)
            o.put("title", s.title)
            o.put("remarks", s.remarks)
            val arr = JSONArray()
            s.rows.forEach { r ->
                arr.put(
                    JSONObject()
                        .put("id", r.id)
                        .put("label", r.label)
                        .put("date", r.date.toString())
                        .put("type", r.type.name)
                        .put("notify", r.notify)
                        .put("createdAt", r.createdAt),
                )
            }
            o.put("rows", arr)
            return o.toString()
        }

        private fun decodeDraft(text: String?): EditorState? {
            if (text == null) return null
            return try {
                val o = JSONObject(text)
                val arr = o.getJSONArray("rows")
                val rows = (0 until arr.length()).map { i ->
                    val r = arr.getJSONObject(i)
                    DateRowState(
                        id = r.getString("id"),
                        label = r.getString("label"),
                        date = LocalDate.parse(r.getString("date")),
                        type = DateType.valueOf(r.getString("type")),
                        notify = r.getBoolean("notify"),
                        createdAt = r.getLong("createdAt"),
                    )
                }
                EditorState(
                    entryId = o.getString("entryId"),
                    isNew = o.getBoolean("isNew"),
                    createdAt = o.getLong("createdAt"),
                    title = o.getString("title"),
                    remarks = o.getString("remarks"),
                    rows = rows,
                    dirty = true,
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
