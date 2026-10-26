package app.cybertracker.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cybertracker.CyberApp
import app.cybertracker.backup.BackupException
import app.cybertracker.backup.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class SettingsViewModel(private val backup: BackupManager) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    fun export(uri: Uri) = launchTask {
        val n = backup.export(uri)
        "Backup saved: $n ${noun(n)} exported (entries, lists and reminders)."
    }

    fun restore(uri: Uri) = launchTask {
        val n = backup.restore(uri)
        "Restore complete: $n ${noun(n)} restored. Reminders were rebuilt."
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun noun(n: Int) = if (n == 1) "entry" else "entries"

    private fun launchTask(block: suspend () -> String) {
        if (_state.value.busy) return
        _state.value = SettingsState(busy = true)
        viewModelScope.launch {
            _state.value = try {
                SettingsState(message = block())
            } catch (e: BackupException) {
                SettingsState(message = e.message ?: "The operation failed.", isError = true)
            } catch (e: Exception) {
                SettingsState(message = "Unexpected error. Your existing data was not changed.", isError = true)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CyberApp
                SettingsViewModel(app.container.backup)
            }
        }
    }
}
