package app.cybertracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cybertracker.data.database.AppDatabase
import app.cybertracker.data.entity.DateType
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.data.entity.EntryEntity
import app.cybertracker.data.entity.EntryWithDates
import app.cybertracker.data.repository.EntryRepository
import app.cybertracker.notifications.AlarmBackend
import app.cybertracker.notifications.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {
    private class FakeBackend : AlarmBackend {
        val active = mutableSetOf<String>()
        override fun set(key: String, triggerAtMillis: Long) {
            active += key
        }

        override fun cancel(key: String) {
            active -= key
        }
    }

    private lateinit var db: AppDatabase
    private lateinit var repo: EntryRepository
    private val backend = FakeBackend()
    private val clock = ZonedDateTime.of(2026, 10, 24, 8, 0, 0, 0, ZoneId.of("UTC"))

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = EntryRepository(db, ReminderScheduler(backend) { clock })
    }

    @After fun tearDown() = db.close()

    private fun entry(id: String, title: String) = EntryEntity(id, title, null, 1L, 1L)
    private fun expiry(id: String, entryId: String, d: LocalDate, notify: Boolean = true) =
        EntryDateEntity(id, entryId, "Expires on", d, DateType.EXPIRY, notify, 1L, 1L)

    @Test fun insertUpdateDeleteRoundTrip() = runBlocking {
        val d = LocalDate.of(2026, 12, 1)
        repo.save(entry("e1", "Mirchi"), listOf(expiry("d1", "e1", d)))
        assertEquals(1, repo.observeAll().first().size)
        assertEquals(d, repo.get("e1")!!.dates.single().date)

        repo.save(entry("e1", "Mirchi 2"), listOf(expiry("d1", "e1", d.plusDays(5)), expiry("d2", "e1", d)))
        val updated = repo.get("e1")!!
        assertEquals("Mirchi 2", updated.entry.title)
        assertEquals(2, updated.dates.size)

        repo.save(entry("e1", "Mirchi 2"), emptyList())
        assertTrue(repo.get("e1")!!.dates.isEmpty())
        assertTrue(backend.active.isEmpty())

        repo.delete("e1")
        assertNull(repo.get("e1"))
    }

    @Test fun deleteCancelsAlarms() = runBlocking {
        repo.save(entry("e1", "A"), listOf(expiry("d1", "e1", LocalDate.of(2026, 12, 1))))
        assertEquals(2, backend.active.size)
        repo.delete("e1")
        assertTrue(backend.active.isEmpty())
    }

    @Test fun failedRestoreLeavesOriginalDataUntouched() = runBlocking {
        repo.save(entry("keep", "Original"), listOf(expiry("kd", "keep", LocalDate.of(2026, 12, 1))))
        val before = repo.snapshot()

        // A date row pointing at a non-existent entry violates the foreign key and aborts the transaction.
        val broken = listOf(
            EntryWithDates(entry("new", "New"), listOf(expiry("x", "missing-entry", LocalDate.of(2026, 12, 2)))),
        )
        try {
            repo.replaceAll(broken)
            fail("Expected the restore to fail")
        } catch (ignored: Exception) {
            // expected
        }
        assertEquals(before, repo.snapshot())
    }
}
