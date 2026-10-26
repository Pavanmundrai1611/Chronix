package app.cybertracker.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.cybertracker.data.entity.CustomReminderEntity
import app.cybertracker.data.entity.ListWithItems
import app.cybertracker.data.entity.ShoppingItemEntity
import app.cybertracker.data.entity.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Transaction
    @Query("SELECT * FROM shopping_lists")
    fun observeAll(): Flow<List<ListWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    fun observe(id: String): Flow<ListWithItems?>

    @Transaction
    @Query("SELECT * FROM shopping_lists")
    suspend fun allOnce(): List<ListWithItems>

    @Upsert
    suspend fun upsertList(list: ShoppingListEntity)

    @Upsert
    suspend fun upsertItem(item: ShoppingItemEntity)

    @Upsert
    suspend fun upsertItems(items: List<ShoppingItemEntity>)

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: String, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND checked = 1")
    suspend fun clearChecked(listId: String)

    @Query("UPDATE shopping_lists SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: String, title: String, now: Long)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteList(id: String)

    @Query("DELETE FROM shopping_lists")
    suspend fun clearAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM custom_reminders")
    fun observeAll(): Flow<List<CustomReminderEntity>>

    @Query("SELECT * FROM custom_reminders WHERE id = :id")
    suspend fun get(id: String): CustomReminderEntity?

    @Query("SELECT * FROM custom_reminders")
    suspend fun allOnce(): List<CustomReminderEntity>

    @Query("SELECT id FROM custom_reminders")
    suspend fun allIds(): List<String>

    @Upsert
    suspend fun upsert(reminder: CustomReminderEntity)

    @Query("DELETE FROM custom_reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM custom_reminders")
    suspend fun clearAll()
}
