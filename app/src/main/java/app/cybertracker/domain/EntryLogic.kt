package app.cybertracker.domain

import app.cybertracker.data.entity.DateType
import app.cybertracker.data.entity.EntryDateEntity
import app.cybertracker.data.entity.EntryWithDates
import java.time.LocalDate

/**
 * [group]: 0 = needs attention, 1 = has an upcoming date, 2 = nothing upcoming.
 * [status] is only non-null when [highlight] is an expiry date.
 */
data class EntrySummary(
    val item: EntryWithDates,
    val highlight: EntryDateEntity?,
    val status: ExpiryStatus?,
    val daysUntil: Long?,
    val group: Int,
)

private val byDateThenId = compareBy<EntryDateEntity>({ it.date }, { it.id })

fun summarize(item: EntryWithDates, today: LocalDate): EntrySummary {
    val expiries = item.dates.filter { it.type == DateType.EXPIRY }

    val attention = expiries
        .filter { expiryStatus(it.date, today).needsAttention }
        .minWithOrNull(byDateThenId)
    if (attention != null) {
        return EntrySummary(item, attention, expiryStatus(attention.date, today), daysUntil(attention.date, today), 0)
    }

    val upcomingExpiry = expiries.filter { !it.date.isBefore(today) }.minWithOrNull(byDateThenId)
    if (upcomingExpiry != null) {
        return EntrySummary(item, upcomingExpiry, ExpiryStatus.SAFE, daysUntil(upcomingExpiry.date, today), 1)
    }

    val upcomingOrdinary = item.dates
        .filter { it.type == DateType.ORDINARY && !it.date.isBefore(today) }
        .minWithOrNull(byDateThenId)
    if (upcomingOrdinary != null) {
        return EntrySummary(item, upcomingOrdinary, null, daysUntil(upcomingOrdinary.date, today), 1)
    }

    val latest = item.dates.maxWithOrNull(byDateThenId)
    return EntrySummary(item, latest, null, latest?.let { daysUntil(it.date, today) }, 2)
}

/** Attention first, then upcoming, then the rest; ties broken by date, title and id. */
val summaryComparator: Comparator<EntrySummary> =
    Comparator<EntrySummary> { a, b ->
        val g = a.group.compareTo(b.group)
        if (g != 0) return@Comparator g
        if (a.group != 2) {
            val da = a.highlight?.date
            val db = b.highlight?.date
            if (da != null && db != null) {
                val c = da.compareTo(db)
                if (c != 0) return@Comparator c
            }
        }
        val t = a.item.entry.title.lowercase().compareTo(b.item.entry.title.lowercase())
        if (t != 0) t else a.item.entry.id.compareTo(b.item.entry.id)
    }

fun matchesQuery(item: EntryWithDates, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return item.entry.title.contains(q, ignoreCase = true) ||
        (item.entry.remarks?.contains(q, ignoreCase = true) == true) ||
        item.dates.any { it.label.contains(q, ignoreCase = true) }
}

fun hasUpcomingExpiry(item: EntryWithDates, today: LocalDate): Boolean =
    item.dates.any { it.type == DateType.EXPIRY && !it.date.isBefore(today) }
