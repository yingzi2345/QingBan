package com.qingban.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RepeatCalculatorTest {

    @Test
    fun `none has no next occurrence`() {
        assertNull(
            RepeatCalculator.nextDate(
                RepeatType.NONE,
                weekdaysMask = 0,
                anchor = LocalDate.of(2026, 10, 29),
                after = LocalDate.of(2026, 10, 29),
            ),
        )
    }

    @Test
    fun `daily advances one day`() {
        assertEquals(
            LocalDate.of(2026, 10, 30),
            RepeatCalculator.nextDate(
                RepeatType.DAILY,
                weekdaysMask = 0,
                anchor = LocalDate.of(2026, 10, 29),
                after = LocalDate.of(2026, 10, 29),
            ),
        )
    }

    @Test
    fun `weekdays skip the weekend`() {
        assertEquals(
            LocalDate.of(2026, 11, 2),
            RepeatCalculator.nextDate(
                RepeatType.WEEKDAYS,
                weekdaysMask = 0,
                anchor = LocalDate.of(2026, 10, 30),
                after = LocalDate.of(2026, 10, 30),
            ),
        )
    }

    @Test
    fun `weekly picks the next selected weekday`() {
        val mask = RepeatCalculator.weekdayMask(1, 3, 5)
        assertEquals(
            LocalDate.of(2026, 10, 30),
            RepeatCalculator.nextDate(
                RepeatType.WEEKLY,
                weekdaysMask = mask,
                anchor = LocalDate.of(2026, 10, 26),
                after = LocalDate.of(2026, 10, 29),
            ),
        )
        assertEquals(
            LocalDate.of(2026, 11, 2),
            RepeatCalculator.nextDate(
                RepeatType.WEEKLY,
                weekdaysMask = mask,
                anchor = LocalDate.of(2026, 10, 26),
                after = LocalDate.of(2026, 10, 30),
            ),
        )
    }

    @Test
    fun `monthly clamps the 31st and keeps the original anchor`() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            RepeatCalculator.nextDate(
                RepeatType.MONTHLY,
                weekdaysMask = 0,
                anchor = LocalDate.of(2025, 1, 31),
                after = LocalDate.of(2025, 1, 31),
            ),
        )
        assertEquals(
            LocalDate.of(2025, 3, 31),
            RepeatCalculator.nextDate(
                RepeatType.MONTHLY,
                weekdaysMask = 0,
                anchor = LocalDate.of(2025, 1, 31),
                after = LocalDate.of(2025, 2, 28),
            ),
        )
    }

    @Test
    fun `yearly handles leap day`() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            RepeatCalculator.nextDate(
                RepeatType.YEARLY,
                weekdaysMask = 0,
                anchor = LocalDate.of(2024, 2, 29),
                after = LocalDate.of(2024, 2, 29),
            ),
        )
        assertEquals(
            LocalDate.of(2028, 2, 29),
            RepeatCalculator.nextDate(
                RepeatType.YEARLY,
                weekdaysMask = 0,
                anchor = LocalDate.of(2024, 2, 29),
                after = LocalDate.of(2027, 2, 28),
            ),
        )
    }
}
