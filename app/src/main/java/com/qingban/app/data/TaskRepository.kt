package com.qingban.app.data

import androidx.room.withTransaction
import com.qingban.app.domain.CompletionResult
import com.qingban.app.domain.OccurrenceStatus
import com.qingban.app.domain.RepeatCalculator
import com.qingban.app.domain.RepeatType
import com.qingban.app.domain.TaskDraft
import com.qingban.app.domain.TaskUi
import com.qingban.app.domain.Quadrant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.max

class TaskRepository(
    private val database: AppDatabase,
) {
    private val dao = database.taskDao()
    private val zoneId = ZoneId.systemDefault()

    val pendingTasks: Flow<List<TaskUi>> =
        dao.observePending().map { records -> records.map { it.toTaskUi() } }

    fun observeTasksBetween(start: LocalDate, endInclusive: LocalDate): Flow<List<TaskUi>> =
        dao.observeBetween(start.toEpochDay(), endInclusive.toEpochDay())
            .map { records -> records.map { it.toTaskUi() } }

    fun observeCompletedOn(date: LocalDate): Flow<List<TaskUi>> {
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return dao.observeCompletedBetween(start, end)
            .map { records -> records.map { it.toTaskUi() } }
    }

    suspend fun createTask(draft: TaskDraft): Long = database.withTransaction {
        val now = System.currentTimeMillis()
        val seriesId = UUID.randomUUID().toString()
        val normalizedMask = normalizeMask(draft)
        val series = TaskSeriesEntity(
            id = seriesId,
            title = draft.title.trim(),
            notes = draft.notes.trim(),
            important = draft.important,
            urgent = draft.urgent,
            timeMinutes = draft.timeMinutes,
            repeatType = draft.repeatType,
            weekdaysMask = normalizedMask,
            anchorEpochDay = draft.dueDate.toEpochDay(),
            createdAt = now,
            updatedAt = now,
        )
        dao.insertSeries(series)
        dao.insertOccurrence(
            TaskOccurrenceEntity(
                seriesId = seriesId,
                dueEpochDay = draft.dueDate.toEpochDay(),
                status = OccurrenceStatus.PENDING,
                completedAt = null,
                createdAt = now,
            ),
        )
    }

    suspend fun updateTask(draft: TaskDraft) = database.withTransaction {
        val seriesId = requireNotNull(draft.seriesId) { "Missing series id" }
        val occurrenceId = requireNotNull(draft.occurrenceId) { "Missing occurrence id" }
        val existing = requireNotNull(dao.getSeries(seriesId)) { "Task no longer exists" }
        val occurrence = requireNotNull(dao.getOccurrence(occurrenceId)) { "Task no longer exists" }
        val now = System.currentTimeMillis()
        val currentDate = LocalDate.ofEpochDay(occurrence.dueEpochDay)
        val repeatChanged = existing.repeatType != draft.repeatType ||
            (draft.repeatType == RepeatType.WEEKLY && existing.weekdaysMask != normalizeMask(draft))
        val dateChanged = currentDate != draft.dueDate
        val newAnchor = if (dateChanged || repeatChanged) draft.dueDate else LocalDate.ofEpochDay(existing.anchorEpochDay)

        dao.updateSeries(
            existing.copy(
                title = draft.title.trim(),
                notes = draft.notes.trim(),
                important = draft.important,
                urgent = draft.urgent,
                timeMinutes = draft.timeMinutes,
                repeatType = draft.repeatType,
                weekdaysMask = normalizeMask(draft),
                anchorEpochDay = newAnchor.toEpochDay(),
                updatedAt = now,
            ),
        )
        if (dateChanged) {
            dao.setOccurrenceDueDate(occurrenceId, draft.dueDate.toEpochDay())
        }
    }

    suspend fun completeTask(task: TaskUi): CompletionResult = database.withTransaction {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        dao.setOccurrenceStatus(task.occurrenceId, OccurrenceStatus.DONE, now)
        val series = dao.getSeries(task.seriesId)
        val nextId = createNextOccurrence(
            series = series,
            after = maxOf(today, task.dueDate),
            now = now,
        )
        CompletionResult(task.occurrenceId, nextId)
    }

    suspend fun undoComplete(result: CompletionResult) = database.withTransaction {
        result.generatedOccurrenceId?.let { generatedId ->
            val generated = dao.getOccurrence(generatedId)
            if (generated?.status == OccurrenceStatus.PENDING) {
                dao.deleteOccurrence(generatedId)
            }
        }
        dao.setOccurrenceStatus(result.occurrenceId, OccurrenceStatus.PENDING, null)
    }

    suspend fun skipTask(task: TaskUi): Long? = database.withTransaction {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        dao.setOccurrenceStatus(task.occurrenceId, OccurrenceStatus.SKIPPED, null)
        val series = dao.getSeries(task.seriesId)
        createNextOccurrence(
            series = series,
            after = maxOf(today, task.dueDate),
            now = now,
        )
    }

    suspend fun moveToToday(task: TaskUi) {
        dao.setOccurrenceDueDate(task.occurrenceId, LocalDate.now().toEpochDay())
    }

    suspend fun moveToQuadrant(task: TaskUi, quadrant: Quadrant) {
        dao.setQuadrant(
            seriesId = task.seriesId,
            important = quadrant.important,
            urgent = quadrant.urgent,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun endRepeat(task: TaskUi) {
        dao.setRepeatType(
            seriesId = task.seriesId,
            repeatType = RepeatType.NONE.name,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun deleteTask(task: TaskUi) {
        dao.deleteSeries(task.seriesId)
    }

    private suspend fun createNextOccurrence(
        series: TaskSeriesEntity?,
        after: LocalDate,
        now: Long,
    ): Long? {
        if (series == null || series.repeatType == RepeatType.NONE) return null
        val anchor = LocalDate.ofEpochDay(series.anchorEpochDay)
        val next = RepeatCalculator.nextDate(
            type = series.repeatType,
            weekdaysMask = series.weekdaysMask,
            anchor = anchor,
            after = after,
        ) ?: return null
        return dao.insertOccurrence(
            TaskOccurrenceEntity(
                seriesId = series.id,
                dueEpochDay = next.toEpochDay(),
                status = OccurrenceStatus.PENDING,
                completedAt = null,
                createdAt = now,
            ),
        )
    }

    private fun normalizeMask(draft: TaskDraft): Int {
        if (draft.repeatType != RepeatType.WEEKLY && draft.repeatType != RepeatType.WEEKDAYS) {
            return 0
        }
        return RepeatCalculator.normalizeWeekdayMask(draft.weekdaysMask, draft.dueDate)
    }
}
