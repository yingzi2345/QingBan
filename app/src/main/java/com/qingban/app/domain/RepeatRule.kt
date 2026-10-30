package com.qingban.app.domain

enum class RepeatType {
    NONE,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

enum class OccurrenceStatus {
    PENDING,
    DONE,
    SKIPPED,
}

enum class Quadrant(
    val important: Boolean,
    val urgent: Boolean,
) {
    DO_NOW(important = true, urgent = true),
    PLAN(important = true, urgent = false),
    QUICK(important = false, urgent = true),
    LATER(important = false, urgent = false);

    companion object {
        fun of(important: Boolean, urgent: Boolean): Quadrant = when {
            important && urgent -> DO_NOW
            important -> PLAN
            urgent -> QUICK
            else -> LATER
        }
    }
}
