package app.cybertracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.cybertracker.CyberApp
import kotlinx.coroutines.launch

/** Fired by AlarmManager for one reminder; not exported. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.data?.lastPathSegment ?: return
        val (dateId, type) = ReminderKeys.parse(key) ?: return
        val pending = goAsync()
        val app = context.applicationContext as CyberApp
        app.container.appScope.launch {
            try {
                NotificationHelper.deliver(context.applicationContext, dateId, type)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Rebuilds alarms after reboot, app update, clock change or time-zone change. Alarms are recomputed
 * from date-only values in the current time zone rather than reusing obsolete timestamps.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as CyberApp
        app.container.appScope.launch {
            try {
                app.container.repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
