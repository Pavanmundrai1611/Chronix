package app.cybertracker.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ExpiryStatus { EXPIRED, EXPIRES_TODAY, EXPIRING_SOON, SAFE }

const val SOON_WINDOW_DAYS = 7L

/** Pure calendar-day comparison; no millisecond arithmetic. */
fun expiryStatus(date: LocalDate, today: LocalDate): ExpiryStatus {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> ExpiryStatus.EXPIRED
        days == 0L -> ExpiryStatus.EXPIRES_TODAY
        days <= SOON_WINDOW_DAYS -> ExpiryStatus.EXPIRING_SOON
        else -> ExpiryStatus.SAFE
    }
}

fun daysUntil(date: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

val ExpiryStatus.needsAttention: Boolean get() = this != ExpiryStatus.SAFE
