package app.cybertracker.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId")],
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val text: String,
    val checked: Boolean,
    val createdAt: Long,
)

data class ListWithItems(
    @Embedded val list: ShoppingListEntity,
    @Relation(parentColumn = "id", entityColumn = "listId")
    val items: List<ShoppingItemEntity>,
)

/** A one-off notification at an exact local date and time (e.g. "Book movie tickets"). */
@Entity(tableName = "custom_reminders")
data class CustomReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String?,
    val date: LocalDate,
    val time: LocalTime,
    val createdAt: Long,
    val updatedAt: Long,
)
