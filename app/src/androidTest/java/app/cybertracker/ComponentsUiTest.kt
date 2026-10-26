package app.cybertracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.cybertracker.domain.ExpiryStatus
import app.cybertracker.ui.components.EmptyState
import app.cybertracker.ui.components.StatusBadge
import app.cybertracker.ui.theme.CyberTheme
import org.junit.Rule
import org.junit.Test

class ComponentsUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun warningStatusesShowTextLabels() {
        rule.setContent {
            CyberTheme {
                StatusBadge(ExpiryStatus.EXPIRING_SOON)
            }
        }
        rule.onNodeWithText("EXPIRING SOON").assertIsDisplayed()
    }

    @Test fun emptyStateIsShown() {
        rule.setContent { CyberTheme { EmptyState("No entries yet", "Add your first entry.") } }
        rule.onNodeWithText("No entries yet").assertIsDisplayed()
    }
}
