package app.cybertracker

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import app.cybertracker.backup.BackupManager
import app.cybertracker.data.database.AppDatabase
import app.cybertracker.data.repository.CustomReminderRepository
import app.cybertracker.data.repository.EntryRepository
import app.cybertracker.data.repository.ShoppingRepository
import app.cybertracker.notifications.AndroidAlarmBackend
import app.cybertracker.notifications.NotificationHelper
import app.cybertracker.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Simple manual dependency container; no DI framework needed for an app this size. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val prefs: SharedPreferences = context.getSharedPreferences("cyber_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.build(context)
    private val scheduler = ReminderScheduler(AndroidAlarmBackend(context))
    val repository = EntryRepository(database, scheduler)
    val shopping = ShoppingRepository(database.shoppingDao())
    val reminders = CustomReminderRepository(database.reminderDao(), scheduler)
    val backup = BackupManager(context.contentResolver, repository)
}

class CyberApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        // Idempotent safety net: rebuild alarms on every process start.
        container.appScope.launch { container.repository.rescheduleAll() }
    }
}
