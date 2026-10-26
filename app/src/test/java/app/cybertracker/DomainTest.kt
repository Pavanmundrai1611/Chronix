package app.cybertracker

import app.cybertracker.data.entity.DateType
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.data.entity.EntryEntity
import app.cybertracker.data.entity.EntryWithDates
import app.cybertracker.domain.ExpiryStatus
import app.cybertracker.domain.expiryStatus
import app.cybertracker.domain.matchesQuery
import app.cybertracker.domain.summarize
import app.cybertracker.domain.summaryComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

internal fun date(id: String, entryId: String, label: String, d: LocalDate, type: DateType, notify: Boolean = true) =
    EntryDateEntity(id, entryId, label, d, type, notify, 0L, 0L)

internal fun entry(id: String, title: String, remarks: String? = null, vararg dates: EntryDateEntity) =
    EntryWithDates(EntryEntity(id, title, remarks, 0L, 0L), dates.toList())

class ExpiryStatusTest {
    private val today = LocalDate.of(2026, 10, 24)

    @Test fun pastDateIsExpired() = assertEquals(ExpiryStatus.EXPIRED, expiryStatus(today.minusDays(1), today))
    @Test fun todayExpiresToday() = assertEquals(ExpiryStatus.EXPIRES_TODAY, expiryStatus(today, today))
    @Test fun oneDayAwayIsSoon() = assertEquals(ExpiryStatus.EXPIRING_SOON, expiryStatus(today.plusDays(1), today))
    @Test fun sevenDaysAwayIsSoon() = assertEquals(ExpiryStatus.EXPIRING_SOON, expiryStatus(today.plusDays(7), today))
    @Test fun eightDaysAwayIsSafe() = assertEquals(ExpiryStatus.SAFE, expiryStatus(today.plusDays(8), today))
    @Test fun crossesYearBoundary() =
        assertEquals(ExpiryStatus.EXPIRING_SOON, expiryStatus(LocalDate.of(2027, 1, 2), LocalDate.of(2026, 12, 30)))
}

class EntryLogicTest {
    private val today = LocalDate.of(2026, 10, 24)

    @Test fun entryWithNoDatesAndNoRemarksIsValid() {
        val s = summarize(entry("1", "Plain"), today)
        assertNull(s.highlight)
        assertNull(s.status)
        assertEquals(2, s.group)
    }

    @Test fun ordinaryDatesNeverShowExpiryStatus() {
        val e = entry("1", "Wash", null, date("d1", "1", "Last done", today.minusDays(3), DateType.ORDINARY))
        assertNull(summarize(e, today).status)
    }

    @Test fun multipleDatesPickMostUrgentExpiry() {
        val e = entry(
            "1", "Insurance", "r",
            date("a", "1", "Purchased", today.minusDays(300), DateType.ORDINARY),
            date("b", "1", "Renewal due", today.plusDays(3), DateType.EXPIRY),
            date("c", "1", "Warranty ends", today.plusDays(200), DateType.EXPIRY),
        )
        val s = summarize(e, today)
        assertEquals("Renewal due", s.highlight?.label)
        assertEquals(ExpiryStatus.EXPIRING_SOON, s.status)
        assertEquals(0, s.group)
    }

    @Test fun orderingIsAttentionThenUpcomingThenRest() {
        val attention = entry("a", "Zeta", null, date("d1", "a", "Exp", today.plusDays(2), DateType.EXPIRY))
        val upcoming = entry("b", "Alpha", null, date("d2", "b", "Exp", today.plusDays(40), DateType.EXPIRY))
        val none = entry("c", "Beta")
        val sorted = listOf(none, upcoming, attention).map { summarize(it, today) }.sortedWith(summaryComparator)
        assertEquals(listOf("a", "b", "c"), sorted.map { it.item.entry.id })
    }

    @Test fun searchMatchesTitleLabelAndRemarks() {
        val e = entry("1", "Mirchi", "keep refrigerated", date("d", "1", "Bought on", today, DateType.ORDINARY))
        assertTrue(matchesQuery(e, "mirc"))
        assertTrue(matchesQuery(e, "BOUGHT"))
        assertTrue(matchesQuery(e, "refriger"))
        assertTrue(!matchesQuery(e, "bike"))
    }
}
