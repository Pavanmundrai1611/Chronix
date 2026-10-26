package app.cybertracker.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

enum class DateType { ORDINARY, EXPIRY }

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val remarks: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** [date] is a calendar date persisted as ISO yyyy-MM-dd text (never a UTC timestamp). */
@Entity(
    tableName = "entry_dates",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId"), Index("date")],
)
data class EntryDateEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val label: String,
    val date: LocalDate,
    val type: DateType,
    val notificationEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class EntryWithDates(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val dates: List<EntryDateEntity>,
)
