package com.qingban.app.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure recurrence math. The series stores an anchor date so monthly and yearly
 * rules keep their original day even when a shorter month clamps the date.
 */
object RepeatCalculator {

    fun nextDate(
        type: RepeatType,
        weekdaysMask: Int,
        anchor: LocalDate,
        after: LocalDate,
    ): LocalDate? = when (type) {
        RepeatType.NONE -> null
        RepeatType.DAILY -> after.plusDays(1)
        RepeatType.WEEKDAYS -> nextWeekday(after)
        RepeatType.WEEKLY -> nextWeekly(weekdaysMask, anchor, after)
        RepeatType.MONTHLY -> nextMonthly(anchor, after)
        RepeatType.YEARLY -> nextYearly(anchor, after)
    }

    fun weekdayMask(vararg days: Int): Int = days.fold(0) { mask, day ->
        mask or (1 shl (day - 1))
    }

    fun dayMask(date: LocalDate): Int = 1 shl (date.dayOfWeek.value - 1)

    fun daySelected(mask: Int, date: LocalDate): Boolean =
        mask and dayMask(date) != 0

    fun normalizeWeekdayMask(mask: Int, fallback: LocalDate): Int =
        if (mask == 0) dayMask(fallback) else mask and 0x7F

    private fun nextWeekday(after: LocalDate): LocalDate {
        var candidate = after.plusDays(1)
        while (candidate.dayOfWeek.value > 5) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun nextWeekly(mask: Int, anchor: LocalDate, after: LocalDate): LocalDate {
        val effectiveMask = normalizeWeekdayMask(mask, anchor)
        var candidate = after.plusDays(1)
        repeat(7) {
            if (daySelected(effectiveMask, candidate)) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun nextMonthly(anchor: LocalDate, after: LocalDate): LocalDate {
        val candidateThisMonth = clampToMonth(after.year, after.monthValue, anchor.dayOfMonth)
        if (candidateThisMonth.isAfter(after)) {
            return candidateThisMonth
        }
        val nextMonth = YearMonth.from(after).plusMonths(1)
        return clampToMonth(nextMonth.year, nextMonth.monthValue, anchor.dayOfMonth)
    }

    private fun nextYearly(anchor: LocalDate, after: LocalDate): LocalDate {
        val candidateThisYear = yearlyDate(after.year, anchor)
        if (candidateThisYear.isAfter(after)) {
            return candidateThisYear
        }
        return yearlyDate(after.year + 1, anchor)
    }

    private fun clampToMonth(year: Int, month: Int, day: Int): LocalDate {
        val lastDay = YearMonth.of(year, month).lengthOfMonth()
        return LocalDate.of(year, month, day.coerceAtMost(lastDay))
    }

    private fun yearlyDate(year: Int, anchor: LocalDate): LocalDate =
        clampToMonth(year, anchor.monthValue, anchor.dayOfMonth)
}
