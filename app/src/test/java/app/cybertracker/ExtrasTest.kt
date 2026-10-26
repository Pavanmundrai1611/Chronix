package app.cybertracker

import app.cybertracker.backup.BackupException
import app.cybertracker.backup.BackupJson
import app.cybertracker.data.entity.CustomReminderEntity
import app.cybertracker.data.entity.ListWithItems
import app.cybertracker.data.entity.ShoppingItemEntity
import app.cybertracker.data.entity.ShoppingListEntity
import app.cybertracker.notifications.AlarmBackend
import app.cybertracker.notifications.ReminderPlanner
import app.cybertracker.notifications.ReminderScheduler
import app.cybertracker.notifications.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class CustomReminderTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = ZonedDateTime.of(2026, 10, 24, 8, 0, 0, 0, zone)

    private fun reminder(date: LocalDate, time: LocalTime) =
        CustomReminderEntity("r1", "Book movie tickets", "Dune", date, time, 0L, 0L)

    private class Fake : AlarmBackend {
        val active = mutableMapOf<String, Long>()
        override fun set(key: String, triggerAtMillis: Long) {
            active[key] = triggerAtMillis
        }

        override fun cancel(key: String) {
            active.remove(key)
        }
    }

    @Test fun futureReminderFiresAtExactLocalTime() {
        val p = ReminderPlanner.planCustom(reminder(LocalDate.of(2026, 10, 25), LocalTime.of(19, 30)), now)
        assertNotNull(p)
        assertEquals(ReminderType.CUSTOM, p!!.type)
        assertEquals(ZonedDateTime.of(2026, 10, 25, 19, 30, 0, 0, zone), p.trigger)
    }

    @Test fun passedReminderIsNotScheduled() {
        assertNull(ReminderPlanner.planCustom(reminder(LocalDate.of(2026, 10, 24), LocalTime.of(7, 59)), now))
        assertNull(ReminderPlanner.planCustom(reminder(LocalDate.of(2026, 10, 24), LocalTime.of(8, 0)), now))
    }

    @Test fun editingMovesTheAlarmAndDeletingCancelsIt() {
        val backend = Fake()
        val s = ReminderScheduler(backend) { now }
        s.scheduleCustom(reminder(LocalDate.of(2026, 10, 25), LocalTime.of(19, 30)))
        val first = backend.active.toMap()
        assertEquals(setOf("r1:CUSTOM"), first.keys)

        s.scheduleCustom(reminder(LocalDate.of(2026, 10, 26), LocalTime.of(10, 0)))
        assertEquals(first.keys, backend.active.keys)
        assertTrue(first != backend.active)

        s.cancel(listOf("r1"))
        assertTrue(backend.active.isEmpty())
    }

    @Test fun editingToThePastCancelsTheAlarm() {
        val backend = Fake()
        val s = ReminderScheduler(backend) { now }
        s.scheduleCustom(reminder(LocalDate.of(2026, 10, 25), LocalTime.of(19, 30)))
        s.scheduleCustom(reminder(LocalDate.of(2026, 10, 1), LocalTime.of(19, 30)))
        assertTrue(backend.active.isEmpty())
    }
}

class BackupV2Test {
    private val list = ListWithItems(
        ShoppingListEntity("l1", "Groceries", 1L, 1L),
        listOf(
            ShoppingItemEntity("i1", "l1", "Milk", true, 1L),
            ShoppingItemEntity("i2", "l1", "Eggs", false, 2L),
        ),
    )
    private val reminder = CustomReminderEntity("r1", "Tickets", null, LocalDate.of(2026, 11, 1), LocalTime.of(18, 45), 1L, 1L)

    @Test fun listsAndRemindersRoundTrip() {
        val text = BackupJson.encode(emptyList(), 0L, listOf(list), listOf(reminder))
        val data = BackupJson.decode(text)
        assertEquals(1, data.lists!!.size)
        val items = data.lists!!.single().items.sortedBy { it.createdAt }
        assertEquals(listOf("Milk", "Eggs"), items.map { it.text })
        assertEquals(listOf(true, false), items.map { it.checked })
        assertEquals(reminder, data.reminders!!.single())
    }

    @Test fun versionOneFilesStillRestoreAndLeaveListsUntouched() {
        val v1 = """{"app":"cyber-date-tracker","version":1,"entries":[]}"""
        val data = BackupJson.decode(v1)
        assertNull(data.lists)
        assertNull(data.reminders)
    }

    @Test fun rejectsInvalidReminderTime() {
        val text = BackupJson.encode(emptyList(), 0L, emptyList(), listOf(reminder)).replace("18:45", "25:99")
        try {
            BackupJson.decode(text)
            fail("Expected BackupException")
        } catch (ignored: BackupException) {
            // expected
        }
    }

    @Test fun rejectsItemWithBlankText() {
        val text = BackupJson.encode(emptyList(), 0L, listOf(list), emptyList()).replace("\"Milk\"", "\"  \"")
        try {
            BackupJson.decode(text)
            fail("Expected BackupException")
        } catch (ignored: BackupException) {
            // expected
        }
    }
}
