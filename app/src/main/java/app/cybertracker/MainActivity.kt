package app.cybertracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.cybertracker.ui.AppNav
import app.cybertracker.ui.theme.CyberTheme

class MainActivity : ComponentActivity() {
    private val pendingEntryId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only react to the launch intent on a fresh start, not after process/config recreation.
        if (savedInstanceState == null) pendingEntryId.value = intent?.getStringExtra(EXTRA_ENTRY_ID)
        setContent {
            CyberTheme {
                AppNav(
                    pendingEntryId = pendingEntryId.value,
                    onPendingConsumed = { pendingEntryId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingEntryId.value = intent.getStringExtra(EXTRA_ENTRY_ID)
    }

    companion object {
        const val EXTRA_ENTRY_ID = "entryId"
    }
}
