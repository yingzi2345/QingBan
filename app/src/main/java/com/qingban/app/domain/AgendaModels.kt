package com.qingban.app.domain

import java.time.LocalDate

/** A calendar entry. A null [startMinutes] means an all-day event. */
data class EventUi(
    val id: String,
    val title: String,
    val notes: String,
    val date: LocalDate,
    val startMinutes: Int?,
    val reminderMinutes: Int?,
    val createdAt: Long,
) {
    val isAllDay: Boolean get() = startMinutes == null
}

data class EventDraft(
    val id: String? = null,
    val title: String,
    val notes: String,
    val date: LocalDate,
    val startMinutes: Int?,
    val reminderMinutes: Int?,
)

/** A countdown card. [yearly] repeats the original month/day every year. */
data class CountdownUi(
    val id: String,
    val title: String,
    val notes: String,
    val targetDate: LocalDate,
    val pinned: Boolean,
    val yearly: Boolean,
    val createdAt: Long,
)

data class CountdownDraft(
    val id: String? = null,
    val title: String,
    val notes: String,
    val targetDate: LocalDate,
    val pinned: Boolean,
    val yearly: Boolean,
)

/** Reminder choices shared by the event and countdown editors. */
enum class ReminderOffset(val minutes: Int?) {
    NONE(null),
    ON_TIME(0),
    FIVE_MINUTES(5),
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    ONE_HOUR(60);

    companion object {
        fun of(minutes: Int?): ReminderOffset =
            entries.firstOrNull { it.minutes == minutes } ?: if (minutes == null) NONE else ON_TIME
    }
}
