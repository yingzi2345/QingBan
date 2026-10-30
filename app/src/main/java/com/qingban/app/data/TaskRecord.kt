package com.qingban.app.data

import com.qingban.app.domain.OccurrenceStatus
import com.qingban.app.domain.RepeatType
import com.qingban.app.domain.TaskUi
import java.time.LocalDate

/**
 * Flat projection of a pending or completed occurrence joined with its series.
 * Querying both tables together keeps Room's invalidation and the UI in sync.
 */
data class TaskRecord(
    val occurrenceId: Long,
    val seriesId: String,
    val dueEpochDay: Long,
    val status: String,
    val completedAt: Long?,
    val title: String,
    val notes: String,
    val important: Boolean,
    val urgent: Boolean,
    val timeMinutes: Int?,
    val repeatType: String,
    val weekdaysMask: Int,
    val anchorEpochDay: Long,
)

fun TaskRecord.toTaskUi(): TaskUi = TaskUi(
    occurrenceId = occurrenceId,
    seriesId = seriesId,
    title = title,
    notes = notes,
    dueDate = LocalDate.ofEpochDay(dueEpochDay),
    timeMinutes = timeMinutes,
    important = important,
    urgent = urgent,
    repeatType = runCatching { RepeatType.valueOf(repeatType) }.getOrDefault(RepeatType.NONE),
    weekdaysMask = weekdaysMask,
    anchorDate = LocalDate.ofEpochDay(anchorEpochDay),
    isCompleted = status == OccurrenceStatus.DONE.name,
    completedAt = completedAt,
)
