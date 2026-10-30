package com.qingban.app.domain

import java.time.LocalDate

data class TaskUi(
    val occurrenceId: Long,
    val seriesId: String,
    val title: String,
    val notes: String,
    val dueDate: LocalDate,
    val timeMinutes: Int?,
    val important: Boolean,
    val urgent: Boolean,
    val repeatType: RepeatType,
    val weekdaysMask: Int,
    val anchorDate: LocalDate,
    val isCompleted: Boolean,
    val completedAt: Long?,
) {
    val quadrant: Quadrant = Quadrant.of(important, urgent)
    val isRecurring: Boolean = repeatType != RepeatType.NONE

    fun isOverdue(today: LocalDate): Boolean = !isCompleted && dueDate.isBefore(today)
}

data class TaskDraft(
    val seriesId: String? = null,
    val occurrenceId: Long? = null,
    val title: String,
    val notes: String,
    val dueDate: LocalDate,
    val timeMinutes: Int?,
    val important: Boolean,
    val urgent: Boolean,
    val repeatType: RepeatType,
    val weekdaysMask: Int,
)

data class CompletionResult(
    val occurrenceId: Long,
    val generatedOccurrenceId: Long?,
)
