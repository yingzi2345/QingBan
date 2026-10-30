package com.qingban.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CountdownCalculatorTest {

    @Test
    fun yearlyCountdownKeepsMonthAndDay() {
        val target = LocalDate.of(1996, 10, 29)
        val today = LocalDate.of(2026, 10, 30)
        assertEquals(
            LocalDate.of(2027, 10, 29),
            CountdownCalculator.nextOccurrence(target, yearly = true, today = today),
        )
    }

    @Test
    fun yearlyCountdownUsesTodayWhenTheDateIsToday() {
        val target = LocalDate.of(2000, 3, 8)
        val today = LocalDate.of(2026, 3, 8)
        assertEquals(
            LocalDate.of(2026, 3, 8),
            CountdownCalculator.nextOccurrence(target, yearly = true, today = today),
        )
        assertEquals(0L, CountdownCalculator.daysFromToday(target, true, today))
    }

    @Test
    fun leapDayFallsBackToFebruary28InCommonYears() {
        val target = LocalDate.of(2024, 2, 29)
        assertEquals(
            LocalDate.of(2027, 2, 28),
            CountdownCalculator.nextOccurrence(target, yearly = true, today = LocalDate.of(2026, 3, 1)),
        )
        assertEquals(
            LocalDate.of(2028, 2, 29),
            CountdownCalculator.nextOccurrence(target, yearly = true, today = LocalDate.of(2027, 3, 1)),
        )
    }

    @Test
    fun nonYearlyCountdownCanBeInThePast() {
        val target = LocalDate.of(2026, 10, 20)
        val today = LocalDate.of(2026, 10, 30)
        assertEquals(-10L, CountdownCalculator.daysFromToday(target, yearly = false, today = today))
    }

    @Test
    fun futureYearlyTargetKeepsItsFirstOccurrence() {
        val target = LocalDate.of(2027, 5, 1)
        val today = LocalDate.of(2026, 10, 30)
        assertEquals(
            LocalDate.of(2027, 5, 1),
            CountdownCalculator.nextOccurrence(target, yearly = true, today = today),
        )
    }
}
