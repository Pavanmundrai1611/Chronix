package app.cybertracker

import app.cybertracker.data.entity.DateType
import app.cybertracker.notifications.AlarmBackend
import app.cybertracker.notifications.ReminderKeys
import app.cybertracker.notifications.ReminderPlanner
import app.cybertracker.notifications.ReminderScheduler
import app.cybertracker.notifications.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private class FakeBackend : AlarmBackend {
    val active = mutableMapOf<String, Long>()
    override fun set(key: String, triggerAtMillis: Long) {
        active[key] = triggerAtMillis
    }

    override fun cancel(key: String) {
        active.remove(key)
    }
}

class SchedulerTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = ZonedDateTime.of(2026, 10, 24, 8, 0, 0, 0, zone)
    private val expiry = LocalDate.of(2026, 11, 20)

    private fun scheduler(backend: FakeBackend, clock: ZonedDateTime = now) = ReminderScheduler(backend) { clock }

    @Test fun schedulesAdvanceAndDueAtNineAm() {
        val plan = ReminderPlanner.plan(date("d1", "e", "Exp", expiry, DateType.EXPIRY), now)
        assertEquals(listOf(ReminderType.ADVANCE, ReminderType.DUE), plan.map { it.type })
        assertEquals(ZonedDateTime.of(2026, 11, 13, 9, 0, 0, 0, zone), plan[0].trigger)
        assertEquals(ZonedDateTime.of(2026, 11, 20, 9, 0, 0, 0, zone), plan[1].trigger)
    }

    @Test fun ordinaryOrDisabledDatesScheduleNothing() {
        assertTrue(ReminderPlanner.plan(date("d1", "e", "x", expiry, DateType.ORDINARY), now).isEmpty())
        assertTrue(ReminderPlanner.plan(date("d1", "e", "x", expiry, DateType.EXPIRY, notify = false), now).isEmpty())
    }

    @Test fun advanceInThePastIsSkippedButDueKept() {
        val plan = ReminderPlanner.plan(date("d1", "e", "x", LocalDate.of(2026, 10, 28), DateType.EXPIRY), now)
        assertEquals(listOf(ReminderType.DUE), plan.map { it.type })
    }

    @Test fun advanceOnTodayIsSkipped() {
        val plan = ReminderPlanner.plan(date("d1", "e", "x", LocalDate.of(2026, 10, 31), DateType.EXPIRY), now)
        assertEquals(listOf(ReminderType.DUE), plan.map { it.type })
    }

    @Test fun expiredDatesScheduleNothing() {
        assertTrue(ReminderPlanner.plan(date("d1", "e", "x", LocalDate.of(2026, 10, 1), DateType.EXPIRY), now).isEmpty())
    }

    @Test fun dueTodayAfterNineAmIsSkipped() {
        val late = ZonedDateTime.of(2026, 10, 24, 10, 0, 0, 0, zone)
        assertTrue(ReminderPlanner.plan(date("d1", "e", "x", late.toLocalDate(), DateType.EXPIRY), late).isEmpty())
        val early = ReminderPlanner.plan(date("d1", "e", "x", now.toLocalDate(), DateType.EXPIRY), now)
        assertEquals(listOf(ReminderType.DUE), early.map { it.type })
    }

    @Test fun keysAreStableAndDistinct() {
        val a = ReminderKeys.key("row-1", ReminderType.ADVANCE)
        assertEquals(a, ReminderKeys.key("row-1", ReminderType.ADVANCE))
        assertNotEquals(a, ReminderKeys.key("row-1", ReminderType.DUE))
        assertNotEquals(a, ReminderKeys.key("row-2", ReminderType.ADVANCE))
        assertEquals("row-1" to ReminderType.ADVANCE, ReminderKeys.parse(a))
    }

    @Test fun changingTheDateReplacesAlarmsWithoutDuplicates() {
        val backend = FakeBackend()
        val s = scheduler(backend)
        s.schedule(listOf(date("d1", "e", "Exp", expiry, DateType.EXPIRY)))
        val first = backend.active.toMap()
        assertEquals(2, first.size)

        s.schedule(listOf(date("d1", "e", "Exp", expiry.plusDays(10), DateType.EXPIRY)))
        assertEquals(first.keys, backend.active.keys)
        assertNotEquals(first, backend.active)
    }

    @Test fun disablingNotificationsCancelsAlarms() {
        val backend = FakeBackend()
        val s = scheduler(backend)
        s.schedule(listOf(date("d1", "e", "Exp", expiry, DateType.EXPIRY)))
        s.schedule(listOf(date("d1", "e", "Exp", expiry, DateType.EXPIRY, notify = false)))
        assertTrue(backend.active.isEmpty())
    }

    @Test fun deletingRowsCancelsOnlyThoseAlarms() {
        val backend = FakeBackend()
        val s = scheduler(backend)
        s.schedule(
            listOf(
                date("d1", "e", "A", expiry, DateType.EXPIRY),
                date("d2", "e", "B", expiry, DateType.EXPIRY),
            ),
        )
        assertEquals(4, backend.active.size)
        s.cancel(listOf("d1"))
        assertEquals(2, backend.active.size)
        assertTrue(backend.active.keys.all { it.startsWith("d2:") })
    }

    @Test fun timeZoneChangeRecomputesFromTheCalendarDate() {
        val tokyo = ZonedDateTime.of(2026, 10, 24, 8, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        val a = ReminderPlanner.plan(date("d1", "e", "x", expiry, DateType.EXPIRY), now)
        val b = ReminderPlanner.plan(date("d1", "e", "x", expiry, DateType.EXPIRY), tokyo)
        assertNotEquals(a[1].triggerAtMillis, b[1].triggerAtMillis)
        assertEquals(9, b[1].trigger.hour)
        assertEquals(expiry, b[1].trigger.toLocalDate())
    }
}
