package app.cybertracker.ui.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cybertracker.CyberApp
import app.cybertracker.data.entity.ListWithItems
import app.cybertracker.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListsState(val loading: Boolean = true, val lists: List<ListWithItems> = emptyList())

class ShoppingListsViewModel(private val repo: ShoppingRepository) : ViewModel() {
    val state: StateFlow<ListsState> = repo.observeAll()
        .map { all ->
            ListsState(
                loading = false,
                lists = all.sortedWith(compareByDescending<ListWithItems> { it.list.createdAt }.thenBy { it.list.id }),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListsState())

    fun create(title: String, onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(repo.createList(title)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CyberApp
                ShoppingListsViewModel(app.container.shopping)
            }
        }
    }
}

sealed interface ListDetailState {
    data object Loading : ListDetailState
    data object NotFound : ListDetailState
    data class Loaded(val data: ListWithItems) : ListDetailState
}

class ListDetailViewModel(
    private val repo: ShoppingRepository,
    handle: SavedStateHandle,
) : ViewModel() {
    private val listId: String = handle.get<String>("listId").orEmpty()

    val state: StateFlow<ListDetailState> = repo.observe(listId)
        .map<ListWithItems?, ListDetailState> { if (it == null) ListDetailState.NotFound else ListDetailState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListDetailState.Loading)

    fun addItem(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repo.addItem(listId, text.take(200)) }
    }

    fun setChecked(itemId: String, checked: Boolean) {
        viewModelScope.launch { repo.setChecked(itemId, checked) }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch { repo.deleteItem(itemId) }
    }

    fun clearChecked() {
        viewModelScope.launch { repo.clearChecked(listId) }
    }

    fun rename(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repo.rename(listId, title.take(100)) }
    }

    fun deleteList() {
        viewModelScope.launch { repo.deleteList(listId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CyberApp
                ListDetailViewModel(app.container.shopping, createSavedStateHandle())
            }
        }
    }
}
