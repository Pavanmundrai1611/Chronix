package app.cybertracker.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cybertracker.CyberApp
import app.cybertracker.data.entity.EntryWithDates
import app.cybertracker.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface DetailsState {
    data object Loading : DetailsState
    data object NotFound : DetailsState
    data class Loaded(val item: EntryWithDates, val today: LocalDate) : DetailsState
}

class DetailsViewModel(
    private val repository: EntryRepository,
    handle: SavedStateHandle,
) : ViewModel() {
    private val entryId: String = handle.get<String>("entryId").orEmpty()
    private val today = MutableStateFlow(LocalDate.now())

    val state: StateFlow<DetailsState> =
        combine(repository.observe(entryId), today) { item, day ->
            if (item == null) DetailsState.NotFound else DetailsState.Loaded(item, day)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsState.Loading)

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(entryId)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CyberApp
                DetailsViewModel(app.container.repository, createSavedStateHandle())
            }
        }
    }
}
