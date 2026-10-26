package app.cybertracker.data.repository

import androidx.room.withTransaction
import app.cybertracker.backup.BackupData
import app.cybertracker.data.database.AppDatabase
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.data.entity.EntryEntity
import app.cybertracker.data.entity.EntryWithDates
import app.cybertracker.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class EntryRepository(
    private val db: AppDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val dao = db.entryDao()

    fun observeAll(): Flow<List<EntryWithDates>> = dao.observeAll()
    fun observe(id: String): Flow<EntryWithDates?> = dao.observe(id)
    suspend fun get(id: String): EntryWithDates? = dao.get(id)
    suspend fun snapshot(): List<EntryWithDates> = dao.allOnce()
    suspend fun snapshotLists() = db.shoppingDao().allOnce()
    suspend fun snapshotReminders() = db.reminderDao().allOnce()
    suspend fun getDate(id: String): EntryDateEntity? = dao.getDate(id)

    /** Creates or updates an entry and its rows atomically, then syncs alarms. */
    suspend fun save(entry: EntryEntity, dates: List<EntryDateEntity>) {
        val keepIds = dates.map { it.id }
        val removed = db.withTransaction {
            val before = dao.dateIdsFor(entry.id)
            dao.upsertEntry(entry)
            dao.deleteDatesNotIn(entry.id, keepIds)
            dao.upsertDates(dates)
            before - keepIds.toSet()
        }
        scheduler.cancel(removed)
        scheduler.schedule(dates)
    }

    suspend fun delete(entryId: String) {
        val ids = db.withTransaction {
            val ids = dao.dateIdsFor(entryId)
            dao.deleteEntry(entryId)
            ids
        }
        scheduler.cancel(ids)
    }

    suspend fun replaceAll(entries: List<EntryWithDates>) = replaceAll(BackupData(entries, null, null))

    /**
     * Replaces the dataset in one transaction; on any failure nothing changes. Shopping lists and
     * reminders are only replaced when the backup contains them (null = leave as is).
     */
    suspend fun replaceAll(data: BackupData) {
        val shopping = db.shoppingDao()
        val reminderDao = db.reminderDao()
        val lists = data.lists
        val reminders = data.reminders
        val oldIds = db.withTransaction {
            val old = dao.allDateIds() + (if (reminders != null) reminderDao.allIds() else emptyList())
            dao.clearAll()
            for (e in data.entries) {
                dao.upsertEntry(e.entry)
                dao.upsertDates(e.dates)
            }
            if (lists != null) {
                shopping.clearAll()
                for (l in lists) {
                    shopping.upsertList(l.list)
                    shopping.upsertItems(l.items)
                }
            }
            if (reminders != null) {
                reminderDao.clearAll()
                for (r in reminders) reminderDao.upsert(r)
            }
            old
        }
        scheduler.cancel(oldIds)
        scheduler.schedule(data.entries.flatMap { it.dates })
        reminders?.forEach { scheduler.scheduleCustom(it) }
    }

    suspend fun rescheduleAll() {
        scheduler.schedule(dao.allDates())
        db.reminderDao().allOnce().forEach { scheduler.scheduleCustom(it) }
    }
}
