package app.cybertracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cybertracker.data.database.AppDatabase
import app.cybertracker.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingInstrumentedTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ShoppingRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = ShoppingRepository(db.shoppingDao())
    }

    @After fun tearDown() = db.close()

    @Test fun checkClearAndDeleteList() = runBlocking {
        val id = repo.createList("Groceries")
        repo.addItem(id, "Milk")
        repo.addItem(id, "Eggs")
        var list = repo.observe(id).first()!!
        assertEquals(2, list.items.size)

        repo.setChecked(list.items.first { it.text == "Milk" }.id, true)
        list = repo.observe(id).first()!!
        assertEquals(1, list.items.count { it.checked })

        repo.clearChecked(id)
        list = repo.observe(id).first()!!
        assertEquals(listOf("Eggs"), list.items.map { it.text })

        repo.deleteList(id)
        assertTrue(repo.observeAll().first().isEmpty())
    }
}
