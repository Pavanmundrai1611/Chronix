package app.cybertracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cybertracker.data.dao.EntryDao
import app.cybertracker.data.dao.ReminderDao
import app.cybertracker.data.dao.ShoppingDao
import app.cybertracker.data.entity.CustomReminderEntity
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.data.entity.EntryEntity
import app.cybertracker.data.entity.ShoppingItemEntity
import app.cybertracker.data.entity.ShoppingListEntity
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }
}

@Database(
    entities = [
        EntryEntity::class,
        EntryDateEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
        CustomReminderEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        /** v1 -> v2: adds shopping lists and custom reminders; existing tables are untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_lists` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_items` (`id` TEXT NOT NULL, `listId` TEXT NOT NULL, " +
                        "`text` TEXT NOT NULL, `checked` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), FOREIGN KEY(`listId`) REFERENCES `shopping_lists`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_listId` ON `shopping_items` (`listId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_reminders` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`note` TEXT, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }

        /** Add a new [Migration] here with every schema bump. No destructive fallback, ever. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cyber_tracker.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
