package app.cybertracker.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.data.entity.EntryEntity
import app.cybertracker.data.entity.EntryWithDates
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Transaction
    @Query("SELECT * FROM entries")
    fun observeAll(): Flow<List<EntryWithDates>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    fun observe(id: String): Flow<EntryWithDates?>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: String): EntryWithDates?

    @Transaction
    @Query("SELECT * FROM entries")
    suspend fun allOnce(): List<EntryWithDates>

    @Query("SELECT * FROM entry_dates")
    suspend fun allDates(): List<EntryDateEntity>

    @Query("SELECT * FROM entry_dates WHERE id = :id")
    suspend fun getDate(id: String): EntryDateEntity?

    @Query("SELECT id FROM entry_dates WHERE entryId = :entryId")
    suspend fun dateIdsFor(entryId: String): List<String>

    @Query("SELECT id FROM entry_dates")
    suspend fun allDateIds(): List<String>

    @Upsert
    suspend fun upsertEntry(entry: EntryEntity)

    @Upsert
    suspend fun upsertDates(dates: List<EntryDateEntity>)

    @Query("DELETE FROM entry_dates WHERE entryId = :entryId AND id NOT IN (:keepIds)")
    suspend fun deleteDatesNotIn(entryId: String, keepIds: List<String>)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("DELETE FROM entries")
    suspend fun clearAll()
}
