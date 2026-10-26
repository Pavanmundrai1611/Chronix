package app.cybertracker.ui

import app.cybertracker.domain.ExpiryStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val formatter: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun prettyDate(date: LocalDate): String = date.format(formatter)

fun prettyTime(time: java.time.LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

fun prettyDateLong(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

fun statusLabel(status: ExpiryStatus): String = when (status) {
    ExpiryStatus.EXPIRED -> "EXPIRED"
    ExpiryStatus.EXPIRES_TODAY -> "EXPIRES TODAY"
    ExpiryStatus.EXPIRING_SOON -> "EXPIRING SOON"
    ExpiryStatus.SAFE -> "SAFE"
}

/** Human wording for a signed day count (negative = in the past). */
fun daysText(days: Long): String = when {
    days == 0L -> "today"
    days == 1L -> "tomorrow"
    days == -1L -> "yesterday"
    days > 1 -> "in $days days"
    else -> "${-days} days ago"
}
