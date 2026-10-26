package app.cybertracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cybertracker.CyberApp
import app.cybertracker.data.repository.EntryRepository
import app.cybertracker.domain.EntrySummary
import app.cybertracker.domain.hasUpcomingExpiry
import app.cybertracker.domain.matchesQuery
import app.cybertracker.domain.summarize
import app.cybertracker.domain.summaryComparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val query: String = "",
    val total: Int = 0,
    val upcomingExpiryCount: Int = 0,
    val attention: List<EntrySummary> = emptyList(),
    val others: List<EntrySummary> = emptyList(),
    /** Flat, sorted results used while a search query is active. */
    val results: List<EntrySummary> = emptyList(),
)

class HomeViewModel(repository: EntryRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val today = MutableStateFlow(LocalDate.now())

    val state: StateFlow<HomeUiState> =
        combine(repository.observeAll(), query, today) { all, q, day ->
            val summaries = all.map { summarize(it, day) }.sortedWith(summaryComparator)
            HomeUiState(
                loading = false,
                query = q,
                total = all.size,
                upcomingExpiryCount = all.count { hasUpcomingExpiry(it, day) },
                attention = summaries.filter { it.group == 0 },
                others = summaries.filter { it.group != 0 },
                results = summaries.filter { matchesQuery(it.item, q) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Called on resume so statuses stay correct after midnight or a time-zone change. */
    fun refreshToday() {
        today.value = LocalDate.now()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CyberApp
                HomeViewModel(app.container.repository)
            }
        }
    }
}
