package app.cybertracker.backup

import app.cybertracker.data.entity.CustomReminderEntity
import app.cybertracker.data.entity.DateType
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.data.entity.EntryEntity
import app.cybertracker.data.entity.EntryWithDates
import app.cybertracker.data.entity.ListWithItems
import app.cybertracker.data.entity.ShoppingItemEntity
import app.cybertracker.data.entity.ShoppingListEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

class BackupException(message: String) : Exception(message)

/** [lists] / [reminders] are null when the file doesn't contain them (version 1): leave current data as is. */
data class BackupData(
    val entries: List<EntryWithDates>,
    val lists: List<ListWithItems>?,
    val reminders: List<CustomReminderEntity>?,
)

/**
 * Backup format v2 (v1 files, which only have "entries", are still accepted):
 * {
 *   "app": "cyber-date-tracker", "version": 2, "exportedAt": "<ISO instant>",
 *   "entries": [ { "id", "title", "remarks"?, "createdAt", "updatedAt",
 *       "dates": [ { "id", "label", "date": "yyyy-MM-dd", "type": "ORDINARY"|"EXPIRY",
 *                    "notificationEnabled": bool, "createdAt", "updatedAt" } ] } ],
 *   "lists": [ { "id", "title", "createdAt", "updatedAt",
 *       "items": [ { "id", "text", "checked": bool, "createdAt" } ] } ],
 *   "reminders": [ { "id", "title", "note"?, "date": "yyyy-MM-dd", "time": "HH:mm", "createdAt", "updatedAt" } ]
 * }
 * Decoding is pure data parsing with strict validation; nothing is evaluated.
 */
object BackupJson {
    const val APP_ID = "cyber-date-tracker"
    const val VERSION = 2
    const val MAX_BYTES = 5 * 1024 * 1024
    const val MAX_ENTRIES = 5000
    const val MAX_DATES_PER_ENTRY = 200
    const val MAX_LISTS = 2000
    const val MAX_ITEMS_PER_LIST = 2000
    const val MAX_REMINDERS = 5000
    const val MAX_TITLE = 200
    const val MAX_LABEL = 100
    const val MAX_REMARKS = 10_000

    fun encode(
        entries: List<EntryWithDates>,
        exportedAtMillis: Long,
        lists: List<ListWithItems> = emptyList(),
        reminders: List<CustomReminderEntity> = emptyList(),
    ): String {
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("version", VERSION)
        root.put("exportedAt", Instant.ofEpochMilli(exportedAtMillis).toString())
        val arr = JSONArray()
        for (e in entries.sortedWith(compareBy({ it.entry.createdAt }, { it.entry.id }))) {
            val o = JSONObject()
            o.put("id", e.entry.id)
            o.put("title", e.entry.title)
            if (e.entry.remarks != null) o.put("remarks", e.entry.remarks)
            o.put("createdAt", e.entry.createdAt)
            o.put("updatedAt", e.entry.updatedAt)
            val dates = JSONArray()
            for (d in e.dates.sortedWith(compareBy({ it.createdAt }, { it.id }))) {
                val dj = JSONObject()
                dj.put("id", d.id)
                dj.put("label", d.label)
                dj.put("date", d.date.toString())
                dj.put("type", d.type.name)
                dj.put("notificationEnabled", d.notificationEnabled)
                dj.put("createdAt", d.createdAt)
                dj.put("updatedAt", d.updatedAt)
                dates.put(dj)
            }
            o.put("dates", dates)
            arr.put(o)
        }
        root.put("entries", arr)

        val listsJson = JSONArray()
        for (l in lists.sortedWith(compareBy({ it.list.createdAt }, { it.list.id }))) {
            val o = JSONObject()
            o.put("id", l.list.id)
            o.put("title", l.list.title)
            o.put("createdAt", l.list.createdAt)
            o.put("updatedAt", l.list.updatedAt)
            val items = JSONArray()
            for (i in l.items.sortedWith(compareBy({ it.createdAt }, { it.id }))) {
                val ij = JSONObject()
                ij.put("id", i.id)
                ij.put("text", i.text)
                ij.put("checked", i.checked)
                ij.put("createdAt", i.createdAt)
                items.put(ij)
            }
            o.put("items", items)
            listsJson.put(o)
        }
        root.put("lists", listsJson)

        val remindersJson = JSONArray()
        for (r in reminders.sortedWith(compareBy({ it.createdAt }, { it.id }))) {
            val o = JSONObject()
            o.put("id", r.id)
            o.put("title", r.title)
            if (r.note != null) o.put("note", r.note)
            o.put("date", r.date.toString())
            o.put("time", r.time.toString())
            o.put("createdAt", r.createdAt)
            o.put("updatedAt", r.updatedAt)
            remindersJson.put(o)
        }
        root.put("reminders", remindersJson)
        return root.toString(2)
    }

    fun decode(text: String): BackupData {
        if (text.length > MAX_BYTES) throw BackupException("The backup file is too large.")
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupException("This file is not valid JSON.")
        }
        if (root.str("app") != APP_ID) throw BackupException("This is not a Cyber Dates backup.")
        val version = root.num("version").toInt()
        if (version != 1 && version != VERSION) throw BackupException("Unsupported backup version $version.")

        val allIds = HashSet<String>()
        val entries = decodeEntries(root.arr("entries"), allIds)
        val lists = if (version >= 2 && root.has("lists")) decodeLists(root.arr("lists"), allIds) else null
        val reminders = if (version >= 2 && root.has("reminders")) decodeReminders(root.arr("reminders"), allIds) else null
        return BackupData(entries, lists, reminders)
    }

    private fun checkTitle(value: String, what: String): String {
        val t = value.trim()
        if (t.isEmpty() || t.length > MAX_TITLE) throw BackupException("A $what has an invalid title or text.")
        return t
    }

    private fun checkId(id: String, seen: MutableSet<String>, what: String) {
        if (id.isBlank() || !seen.add(id)) throw BackupException("A $what has a missing or duplicate id.")
    }

    private fun decodeEntries(entriesJson: JSONArray, seen: MutableSet<String>): List<EntryWithDates> {
        if (entriesJson.length() > MAX_ENTRIES) throw BackupException("The backup has too many entries.")
        val result = ArrayList<EntryWithDates>(entriesJson.length())
        for (i in 0 until entriesJson.length()) {
            val o = entriesJson.obj(i)
            val id = o.str("id")
            checkId(id, seen, "entry")
            val title = checkTitle(o.str("title"), "entry")
            val remarks = o.optStr("remarks")
            if (remarks != null && remarks.length > MAX_REMARKS) throw BackupException("Entry \"$title\" has overly long remarks.")
            val datesJson = o.arr("dates")
            if (datesJson.length() > MAX_DATES_PER_ENTRY) throw BackupException("Entry \"$title\" has too many dates.")
            val dates = ArrayList<EntryDateEntity>(datesJson.length())
            for (j in 0 until datesJson.length()) {
                val d = datesJson.obj(j)
                val dId = d.str("id")
                checkId(dId, seen, "date row")
                val label = d.str("label").trim()
                if (label.isEmpty() || label.length > MAX_LABEL) throw BackupException("Entry \"$title\" has an invalid date label.")
                val date = parseDate(d.str("date"), title)
                val typeName = d.str("type")
                val type = DateType.values().firstOrNull { it.name == typeName }
                    ?: throw BackupException("Entry \"$title\" has an unknown date type.")
                dates += EntryDateEntity(
                    id = dId,
                    entryId = id,
                    label = label,
                    date = date,
                    type = type,
                    notificationEnabled = d.bool("notificationEnabled"),
                    createdAt = d.num("createdAt").toLong(),
                    updatedAt = d.num("updatedAt").toLong(),
                )
            }
            val entry = EntryEntity(
                id = id,
                title = title,
                remarks = remarks?.ifBlank { null },
                createdAt = o.num("createdAt").toLong(),
                updatedAt = o.num("updatedAt").toLong(),
            )
            result += EntryWithDates(entry, dates)
        }
        return result
    }

    private fun decodeLists(listsJson: JSONArray, seen: MutableSet<String>): List<ListWithItems> {
        if (listsJson.length() > MAX_LISTS) throw BackupException("The backup has too many shopping lists.")
        val result = ArrayList<ListWithItems>(listsJson.length())
        for (i in 0 until listsJson.length()) {
            val o = listsJson.obj(i)
            val id = o.str("id")
            checkId(id, seen, "shopping list")
            val title = checkTitle(o.str("title"), "shopping list")
            val itemsJson = o.arr("items")
            if (itemsJson.length() > MAX_ITEMS_PER_LIST) throw BackupException("List \"$title\" has too many items.")
            val items = ArrayList<ShoppingItemEntity>(itemsJson.length())
            for (j in 0 until itemsJson.length()) {
                val it = itemsJson.obj(j)
                val itemId = it.str("id")
                checkId(itemId, seen, "list item")
                items += ShoppingItemEntity(
                    id = itemId,
                    listId = id,
                    text = checkTitle(it.str("text"), "list item"),
                    checked = it.bool("checked"),
                    createdAt = it.num("createdAt").toLong(),
                )
            }
            result += ListWithItems(
                ShoppingListEntity(id, title, o.num("createdAt").toLong(), o.num("updatedAt").toLong()),
                items,
            )
        }
        return result
    }

    private fun decodeReminders(remindersJson: JSONArray, seen: MutableSet<String>): List<CustomReminderEntity> {
        if (remindersJson.length() > MAX_REMINDERS) throw BackupException("The backup has too many reminders.")
        val result = ArrayList<CustomReminderEntity>(remindersJson.length())
        for (i in 0 until remindersJson.length()) {
            val o = remindersJson.obj(i)
            val id = o.str("id")
            checkId(id, seen, "reminder")
            val title = checkTitle(o.str("title"), "reminder")
            val note = o.optStr("note")
            if (note != null && note.length > MAX_REMARKS) throw BackupException("Reminder \"$title\" has an overly long note.")
            val time = try {
                LocalTime.parse(o.str("time"))
            } catch (e: DateTimeParseException) {
                throw BackupException("Reminder \"$title\" has an invalid time.")
            }
            result += CustomReminderEntity(
                id = id,
                title = title,
                note = note?.ifBlank { null },
                date = parseDate(o.str("date"), title),
                time = time,
                createdAt = o.num("createdAt").toLong(),
                updatedAt = o.num("updatedAt").toLong(),
            )
        }
        return result
    }

    private fun parseDate(text: String, owner: String): LocalDate = try {
        LocalDate.parse(text)
    } catch (e: DateTimeParseException) {
        throw BackupException("\"$owner\" has an invalid date.")
    }

    private fun JSONObject.value(name: String): Any {
        if (!has(name) || isNull(name)) throw BackupException("Missing required field \"$name\".")
        return get(name)
    }

    private fun JSONObject.str(name: String): String =
        value(name) as? String ?: throw BackupException("Field \"$name\" must be text.")

    private fun JSONObject.optStr(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return get(name) as? String ?: throw BackupException("Field \"$name\" must be text.")
    }

    private fun JSONObject.num(name: String): Number =
        value(name) as? Number ?: throw BackupException("Field \"$name\" must be a number.")

    private fun JSONObject.bool(name: String): Boolean =
        value(name) as? Boolean ?: throw BackupException("Field \"$name\" must be true or false.")

    private fun JSONObject.arr(name: String): JSONArray =
        value(name) as? JSONArray ?: throw BackupException("Field \"$name\" must be a list.")

    private fun JSONArray.obj(i: Int): JSONObject =
        opt(i) as? JSONObject ?: throw BackupException("List item ${i + 1} is not an object.")
}
