package app.cybertracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.cybertracker.CyberApp
import app.cybertracker.MainActivity
import app.cybertracker.R
import app.cybertracker.data.entity.DateType
import app.cybertracker.ui.prettyDate
import app.cybertracker.ui.prettyTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object NotificationHelper {
    const val CHANNEL_ID = "expiry_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Expiry reminders and your own timed reminders"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** True only if the app may actually show notifications right now. */
    fun areEnabled(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Looks up current data when the alarm fires, so stale alarms never show outdated text. */
    suspend fun deliver(context: Context, id: String, type: ReminderType) {
        if (!areEnabled(context)) return
        if (type == ReminderType.CUSTOM) deliverCustom(context, id) else deliverExpiry(context, id, type)
    }

    private suspend fun deliverCustom(context: Context, id: String) {
        val app = context.applicationContext as CyberApp
        val r = app.container.reminders.get(id) ?: return
        val body = buildString {
            append(prettyTime(r.time))
            if (!r.note.isNullOrBlank()) append(" · ").append(r.note)
        }
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val notificationId = ReminderKeys.key(id, ReminderType.CUSTOM).hashCode()
        val contentIntent = PendingIntent.getActivity(
            context, notificationId, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(r.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF19F0FF.toInt())
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (ignored: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    private suspend fun deliverExpiry(context: Context, dateId: String, type: ReminderType) {
        val app = context.applicationContext as CyberApp
        val repo = app.container.repository
        val row = repo.getDate(dateId) ?: return
        if (row.type != DateType.EXPIRY || !row.notificationEnabled) return
        val entry = repo.get(row.entryId)?.entry ?: return

        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(today, row.date)
        if (days < 0) return

        val whenText = when (days) {
            0L -> "today"
            1L -> "tomorrow"
            else -> "in $days days"
        }
        val title = if (type == ReminderType.DUE || days == 0L) "${entry.title} expires today" else "${entry.title} expires $whenText"
        val body = "${row.label}: ${prettyDate(row.date)}"

        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ENTRY_ID, entry.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val notificationId = ReminderKeys.key(dateId, type).hashCode()
        val contentIntent = PendingIntent.getActivity(
            context, notificationId, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFFF2BD6.toInt())
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (ignored: SecurityException) {
            // Permission revoked between the check and the call; nothing else to do.
        }
    }
}
