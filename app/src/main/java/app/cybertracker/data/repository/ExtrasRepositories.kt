package app.cybertracker.data.repository

import app.cybertracker.data.dao.ReminderDao
import app.cybertracker.data.dao.ShoppingDao
import app.cybertracker.data.entity.CustomReminderEntity
import app.cybertracker.data.entity.ListWithItems
import app.cybertracker.data.entity.ShoppingItemEntity
import app.cybertracker.data.entity.ShoppingListEntity
import app.cybertracker.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Shopping lists persist every change immediately; there is no separate save step. */
class ShoppingRepository(private val dao: ShoppingDao) {
    fun observeAll(): Flow<List<ListWithItems>> = dao.observeAll()
    fun observe(id: String): Flow<ListWithItems?> = dao.observe(id)

    suspend fun createList(title: String): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        dao.upsertList(ShoppingListEntity(id, title.trim(), now, now))
        return id
    }

    suspend fun rename(id: String, title: String) = dao.rename(id, title.trim(), System.currentTimeMillis())
    suspend fun deleteList(id: String) = dao.deleteList(id)

    suspend fun addItem(listId: String, text: String) {
        dao.upsertItem(ShoppingItemEntity(UUID.randomUUID().toString(), listId, text.trim(), false, System.currentTimeMillis()))
    }

    suspend fun setChecked(itemId: String, checked: Boolean) = dao.setChecked(itemId, checked)
    suspend fun deleteItem(itemId: String) = dao.deleteItem(itemId)
    suspend fun clearChecked(listId: String) = dao.clearChecked(listId)
}

class CustomReminderRepository(
    private val dao: ReminderDao,
    private val scheduler: ReminderScheduler,
) {
    fun observeAll(): Flow<List<CustomReminderEntity>> = dao.observeAll()
    suspend fun get(id: String): CustomReminderEntity? = dao.get(id)
    suspend fun getAll(): List<CustomReminderEntity> = dao.allOnce()

    suspend fun save(reminder: CustomReminderEntity) {
        dao.upsert(reminder)
        scheduler.scheduleCustom(reminder)
    }

    suspend fun delete(id: String) {
        dao.delete(id)
        scheduler.cancel(listOf(id))
    }
}
