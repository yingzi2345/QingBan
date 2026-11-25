package com.qingban.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderTimeCalculatorTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun taskWithExplicitTimeUsesThatMinute() {
        val date = LocalDate.of(2026, 11, 25)
        val trigger = ReminderTimeCalculator.taskTriggerMillis(
            dueEpochDay = date.toEpochDay(),
            timeMinutes = 14 * 60 + 30,
            zone = zone,
        )
        assertEquals(
            date.atTime(14, 30).atZone(zone).toInstant().toEpochMilli(),
            trigger,
        )
    }

    @Test
    fun taskWithoutTimeDefaultsToNineAm() {
        val date = LocalDate.of(2026, 11, 26)
        val trigger = ReminderTimeCalculator.taskTriggerMillis(
            dueEpochDay = date.toEpochDay(),
            timeMinutes = null,
            zone = zone,
        )
        assertEquals(
            date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            trigger,
        )
    }

    @Test
    fun eventReminderIsBeforeStartAndNullMeansNoReminder() {
        val date = LocalDate.of(2026, 11, 25)
        val start = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val trigger = ReminderTimeCalculator.eventTriggerMillis(
            startEpochDay = date.toEpochDay(),
            startMinutes = 10 * 60,
            reminderMinutes = 15,
            zone = zone,
        )
        assertEquals(start - 15 * 60_000L, trigger)

        assertNull(
            ReminderTimeCalculator.eventTriggerMillis(
                startEpochDay = date.toEpochDay(),
                startMinutes = 10 * 60,
                reminderMinutes = null,
                zone = zone,
            ),
        )
    }

    @Test
    fun allDayEventReminderFiresAtDefaultHour() {
        val date = LocalDate.of(2026, 11, 25)
        val trigger = ReminderTimeCalculator.eventTriggerMillis(
            startEpochDay = date.toEpochDay(),
            startMinutes = null,
            reminderMinutes = 0,
            zone = zone,
        )
        assertEquals(
            date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            trigger,
        )
    }

    @Test
    fun slightlyOverdueReminderIsKeptAndClampedToTheFuture() {
        val now = 1_700_000_000_000L
        val trigger = now - 3 * 60_000L
        assertTrue(ReminderTimeCalculator.isRelevant(trigger, now))
        assertEquals(
            now + ReminderTimeCalculator.MIN_FUTURE_DELAY_SECONDS * 1000L,
            ReminderTimeCalculator.effectiveTriggerAt(trigger, now),
        )
    }

    @Test
    fun farPastReminderIsDiscarded() {
        val now = 1_700_000_000_000L
        assertFalse(
            ReminderTimeCalculator.isRelevant(
                now - (ReminderTimeCalculator.PAST_GRACE_MINUTES + 1) * 60_000L,
                now,
            ),
        )
    }

    @Test
    fun futureReminderKeepsItsOriginalTrigger() {
        val now = 1_700_000_000_000L
        val trigger = now + 5 * 60_000L
        assertTrue(ReminderTimeCalculator.isRelevant(trigger, now))
        assertEquals(trigger, ReminderTimeCalculator.effectiveTriggerAt(trigger, now))
    }
}
