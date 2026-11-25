package com.qingban.app.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure time math for local reminders. Kept free of Android APIs so the trigger
 * rules can be verified with JVM unit tests.
 */
object ReminderTimeCalculator {

    /** Tasks and all-day events without an explicit time fall back to 09:00. */
    const val DEFAULT_HOUR = 9

    /**
     * An alarm whose trigger time has just passed may not have been delivered yet
     * (inexact alarms and Doze can delay it). Keep it for this window and fire it
     * shortly after the refresh instead of dropping it.
     */
    const val PAST_GRACE_MINUTES = 10L

    /** Minimum delay used when re-scheduling a remind-later alarm that is already due. */
    const val MIN_FUTURE_DELAY_SECONDS = 2L

    fun taskTriggerMillis(dueEpochDay: Long, timeMinutes: Int?, zone: ZoneId): Long {
        val minutes = timeMinutes ?: (DEFAULT_HOUR * 60)
        return LocalDate.ofEpochDay(dueEpochDay)
            .atTime(minutes / 60, minutes % 60)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    /** Returns null when the event has no reminder configured. */
    fun eventTriggerMillis(
        startEpochDay: Long,
        startMinutes: Int?,
        reminderMinutes: Int?,
        zone: ZoneId,
    ): Long? {
        if (reminderMinutes == null) return null
        val minutes = startMinutes ?: (DEFAULT_HOUR * 60)
        return LocalDate.ofEpochDay(startEpochDay)
            .atTime(minutes / 60, minutes % 60)
            .atZone(zone)
            .toInstant()
            .toEpochMilli() - reminderMinutes * 60_000L
    }

    fun countdownTriggerMillis(
        target: LocalDate,
        yearly: Boolean,
        today: LocalDate,
        zone: ZoneId,
    ): Long {
        val occurrence = CountdownCalculator.nextOccurrence(target, yearly, today)
        return occurrence.atTime(DEFAULT_HOUR, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    /** A slightly overdue reminder is still relevant; old ones are ignored. */
    fun isRelevant(triggerAt: Long, now: Long): Boolean =
        triggerAt >= now - PAST_GRACE_MINUTES * 60_000L

    /**
     * AlarmManager needs a future trigger time. Clamp an overdue reminder to a
     * small delay so it is delivered instead of being silently discarded.
     */
    fun effectiveTriggerAt(triggerAt: Long, now: Long): Long =
        maxOf(triggerAt, now + MIN_FUTURE_DELAY_SECONDS * 1000L)
}
