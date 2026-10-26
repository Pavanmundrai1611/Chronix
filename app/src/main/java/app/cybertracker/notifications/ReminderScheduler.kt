package app.cybertracker.notifications

import app.cybertracker.data.entity.CustomReminderEntity
import app.cybertracker.data.entity.DateType
import app.cybertracker.data.entity.EntryDateEntity
import java.time.LocalTime
import java.time.ZonedDateTime

enum class ReminderType { ADVANCE, DUE, CUSTOM }

/** Stable alarm identity: "<dateRowId>:<TYPE>". Unique per row and reminder kind. */
object ReminderKeys {
    fun key(dateId: String, type: ReminderType) = "$dateId:${type.name}"

    fun parse(key: String): Pair<String, ReminderType>? {
        val idx = key.lastIndexOf(':')
        if (idx <= 0) return null
        val type = ReminderType.values().firstOrNull { it.name == key.substring(idx + 1) } ?: return null
        return key.substring(0, idx) to type
    }
}

data class PlannedReminder(val dateId: String, val type: ReminderType, val trigger: ZonedDateTime) {
    val key: String get() = ReminderKeys.key(dateId, type)
    val triggerAtMillis: Long get() = trigger.toInstant().toEpochMilli()
}

/** Pure scheduling math. Recomputed from the date-only value in the *current* time zone. */
object ReminderPlanner {
    val NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)
    const val ADVANCE_DAYS = 7L

    fun plan(date: EntryDateEntity, now: ZonedDateTime): List<PlannedReminder> {
        if (date.type != DateType.EXPIRY || !date.notificationEnabled) return emptyList()
        val zone = now.zone
        val today = now.toLocalDate()
        val result = mutableListOf<PlannedReminder>()

        // Only a still-relevant advance reminder: its day must be strictly in the future.
        val advanceDay = date.date.minusDays(ADVANCE_DAYS)
        if (advanceDay.isAfter(today)) {
            result += PlannedReminder(date.id, ReminderType.ADVANCE, advanceDay.atTime(NOTIFY_TIME).atZone(zone))
        }
        // Expiry-day reminder: never for expired dates or a 9:00 that already passed.
        val due = date.date.atTime(NOTIFY_TIME).atZone(zone)
        if (!date.date.isBefore(today) && due.isAfter(now)) {
            result += PlannedReminder(date.id, ReminderType.DUE, due)
        }
        return result
    }

    /** One-off reminder at its exact local date and time; null if that moment has passed. */
    fun planCustom(reminder: CustomReminderEntity, now: ZonedDateTime): PlannedReminder? {
        val at = reminder.date.atTime(reminder.time).atZone(now.zone)
        return if (at.isAfter(now)) PlannedReminder(reminder.id, ReminderType.CUSTOM, at) else null
    }
}

/** Platform abstraction so scheduling logic is testable without Android. */
interface AlarmBackend {
    fun set(key: String, triggerAtMillis: Long)
    fun cancel(key: String)
}

class ReminderScheduler(
    private val backend: AlarmBackend,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    /** Idempotent: clears both alarms of each row, then sets whatever is still relevant. */
    fun schedule(dates: List<EntryDateEntity>) {
        val now = clock()
        for (d in dates) {
            cancelOne(d.id)
            ReminderPlanner.plan(d, now).forEach { backend.set(it.key, it.triggerAtMillis) }
        }
    }

    fun scheduleCustom(reminder: CustomReminderEntity) {
        cancelOne(reminder.id)
        ReminderPlanner.planCustom(reminder, clock())?.let { backend.set(it.key, it.triggerAtMillis) }
    }

    fun cancel(dateIds: Collection<String>) = dateIds.forEach(::cancelOne)

    private fun cancelOne(dateId: String) {
        ReminderType.values().forEach { backend.cancel(ReminderKeys.key(dateId, it)) }
    }
}
