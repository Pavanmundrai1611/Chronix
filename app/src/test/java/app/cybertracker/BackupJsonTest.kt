package app.cybertracker

import app.cybertracker.backup.BackupException
import app.cybertracker.backup.BackupJson
import app.cybertracker.data.entity.DateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class BackupJsonTest {
    private val sample = listOf(
        entry(
            "e1", "Mirchi", "Opened packet",
            date("d1", "e1", "Bought on", LocalDate.of(2026, 10, 12), DateType.ORDINARY),
            date("d2", "e1", "Expires on", LocalDate.of(2026, 10, 31), DateType.EXPIRY),
        ),
        entry("e2", "No dates, no remarks"),
    )

    private fun assertRejected(text: String) {
        try {
            BackupJson.decode(text)
            fail("Expected BackupException for: $text")
        } catch (ignored: BackupException) {
            // expected
        }
    }

    @Test fun roundTripPreservesEverything() {
        val decoded = BackupJson.decode(BackupJson.encode(sample, 1_700_000_000_000L)).entries
        assertEquals(2, decoded.size)
        val mirchi = decoded.first { it.entry.id == "e1" }
        assertEquals("Opened packet", mirchi.entry.remarks)
        assertEquals(2, mirchi.dates.size)
        val expiry = mirchi.dates.first { it.id == "d2" }
        assertEquals(LocalDate.of(2026, 10, 31), expiry.date)
        assertEquals(DateType.EXPIRY, expiry.type)
        assertEquals(true, expiry.notificationEnabled)
        assertEquals("Expires on", expiry.label)
        val bare = decoded.first { it.entry.id == "e2" }
        assertNull(bare.entry.remarks)
        assertEquals(0, bare.dates.size)
    }

    @Test fun rejectsMalformedJson() = assertRejected("{not json")
    @Test fun rejectsWrongApp() = assertRejected("""{"app":"other","version":1,"entries":[]}""")
    @Test fun rejectsUnsupportedVersion() = assertRejected("""{"app":"cyber-date-tracker","version":99,"entries":[]}""")
    @Test fun rejectsMissingEntries() = assertRejected("""{"app":"cyber-date-tracker","version":1}""")

    @Test fun rejectsInvalidDate() {
        val good = BackupJson.encode(sample, 0L)
        assertRejected(good.replace("2026-10-31", "2026-13-45"))
    }

    @Test fun rejectsUnknownDateType() {
        val good = BackupJson.encode(sample, 0L)
        assertRejected(good.replace("\"EXPIRY\"", "\"WHATEVER\""))
    }

    @Test fun rejectsDuplicateIds() {
        val dup = listOf(entry("e1", "A"), entry("e1", "B"))
        assertRejected(BackupJson.encode(dup, 0L))
    }

    @Test fun rejectsBlankTitle() {
        val good = BackupJson.encode(listOf(entry("e1", "Title")), 0L)
        assertRejected(good.replace("\"Title\"", "\"   \""))
    }

    @Test fun rejectsOversizedInput() {
        assertRejected(" ".repeat(BackupJson.MAX_BYTES + 1))
    }
}
