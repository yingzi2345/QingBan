package com.qingban.app.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Countdown date math. Yearly countdowns keep the original month/day; Feb 29
 * falls back to Feb 28 in non-leap years.
 */
object CountdownCalculator {

    /** The next date the countdown points at, which may be in the past when not yearly. */
    fun nextOccurrence(target: LocalDate, yearly: Boolean, today: LocalDate): LocalDate {
        if (!yearly) return target
        // A yearly countdown can have a target date in the future (the first occurrence).
        var year = maxOf(target.year, today.year)
        var candidate = occurrenceInYear(target, year)
        while (candidate.isBefore(today)) {
            year += 1
            candidate = occurrenceInYear(target, year)
        }
        return candidate
    }

    fun daysFromToday(target: LocalDate, yearly: Boolean, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, nextOccurrence(target, yearly, today))

    fun occurrenceInYear(target: LocalDate, year: Int): LocalDate {
        val lastDay = YearMonth.of(year, target.monthValue).lengthOfMonth()
        return LocalDate.of(year, target.monthValue, target.dayOfMonth.coerceAtMost(lastDay))
    }
}
