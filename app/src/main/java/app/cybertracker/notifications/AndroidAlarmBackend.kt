package app.cybertracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Inexact, Doze-friendly alarms via [AlarmManager.setAndAllowWhileIdle]. This needs no special
 * permission (unlike exact alarms on Android 12+). Android may delay delivery by some minutes.
 *
 * Each alarm is identified by a unique data URI built from the stable key, so PendingIntents for
 * different rows/reminder types can never overwrite each other.
 */
class AndroidAlarmBackend(context: Context) : AlarmBackend {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun set(key: String, triggerAtMillis: Long) {
        val pi = pendingIntent(key, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
    }

    override fun cancel(key: String) {
        val pi = pendingIntent(key, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(key: String, flag: Int): PendingIntent? {
        val intent = Intent(appContext, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .setData(Uri.parse("cybertracker://reminder/" + Uri.encode(key)))
        return PendingIntent.getBroadcast(appContext, 0, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_REMINDER = "app.cybertracker.action.REMINDER"
    }
}
