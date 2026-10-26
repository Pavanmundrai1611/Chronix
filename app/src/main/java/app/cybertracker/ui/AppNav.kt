package app.cybertracker.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.cybertracker.ui.details.DetailsScreen
import app.cybertracker.ui.details.DetailsViewModel
import app.cybertracker.ui.editor.EditorScreen
import app.cybertracker.ui.editor.EditorViewModel
import app.cybertracker.ui.home.HomeScreen
import app.cybertracker.ui.home.HomeViewModel
import app.cybertracker.ui.lists.ListDetailScreen
import app.cybertracker.ui.lists.ListDetailViewModel
import app.cybertracker.ui.lists.ShoppingListsViewModel
import app.cybertracker.ui.reminders.ReminderEditorScreen
import app.cybertracker.ui.reminders.ReminderEditorViewModel
import app.cybertracker.ui.reminders.RemindersViewModel
import app.cybertracker.ui.settings.SettingsScreen
import app.cybertracker.ui.settings.SettingsViewModel

private const val HOME = "home"
private const val SETTINGS = "settings"
private const val DETAILS = "details/{entryId}"
private const val EDITOR = "editor?entryId={entryId}"
private const val LIST = "list/{listId}"
private const val REMINDER = "reminder?reminderId={reminderId}"

@Composable
fun AppNav(pendingEntryId: String?, onPendingConsumed: () -> Unit) {
    val nav = rememberNavController()

    // Tapping a reminder notification opens the relevant entry.
    LaunchedEffect(pendingEntryId) {
        if (pendingEntryId != null) {
            nav.navigate("details/$pendingEntryId") { launchSingleTop = true }
            onPendingConsumed()
        }
    }

    NavHost(
        navController = nav,
        startDestination = HOME,
        enterTransition = { slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(320)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(320)) },
        popExitTransition = { slideOutHorizontally(tween(260)) { it / 5 } + fadeOut(tween(260)) },
    ) {
        composable(HOME) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            val listsVm: ShoppingListsViewModel = viewModel(factory = ShoppingListsViewModel.Factory)
            val remindersVm: RemindersViewModel = viewModel(factory = RemindersViewModel.Factory)
            HomeScreen(
                vm = vm,
                listsVm = listsVm,
                remindersVm = remindersVm,
                onAddEntry = { nav.navigate("editor") },
                onOpenEntry = { id -> nav.navigate("details/$id") },
                onOpenList = { id -> nav.navigate("list/$id") },
                onAddReminder = { nav.navigate("reminder") },
                onOpenReminder = { id -> nav.navigate("reminder?reminderId=$id") },
                onSettings = { nav.navigate(SETTINGS) },
            )
        }
        composable(
            route = DETAILS,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
        ) {
            val vm: DetailsViewModel = viewModel(factory = DetailsViewModel.Factory)
            DetailsScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate("editor?entryId=$id") },
            )
        }
        composable(
            route = EDITOR,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            val vm: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
            EditorScreen(vm = vm, onClose = { nav.popBackStack() })
        }
        composable(
            route = LIST,
            arguments = listOf(navArgument("listId") { type = NavType.StringType }),
        ) {
            val vm: ListDetailViewModel = viewModel(factory = ListDetailViewModel.Factory)
            ListDetailScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable(
            route = REMINDER,
            arguments = listOf(
                navArgument("reminderId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            val vm: ReminderEditorViewModel = viewModel(factory = ReminderEditorViewModel.Factory)
            ReminderEditorScreen(vm = vm, onClose = { nav.popBackStack() })
        }
        composable(SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
