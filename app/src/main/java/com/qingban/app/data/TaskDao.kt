package com.qingban.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qingban.app.domain.OccurrenceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insertSeries(series: TaskSeriesEntity)

    @Insert
    suspend fun insertOccurrence(occurrence: TaskOccurrenceEntity): Long

    @Update
    suspend fun updateSeries(series: TaskSeriesEntity)

    @Update
    suspend fun updateOccurrence(occurrence: TaskOccurrenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeriesList(series: List<TaskSeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrenceList(occurrences: List<TaskOccurrenceEntity>)

    @Query("SELECT * FROM task_series WHERE id = :seriesId LIMIT 1")
    suspend fun getSeries(seriesId: String): TaskSeriesEntity?

    @Query("SELECT * FROM task_series ORDER BY createdAt ASC")
    suspend fun getAllSeries(): List<TaskSeriesEntity>

    @Query("SELECT * FROM task_occurrences ORDER BY dueEpochDay ASC, id ASC")
    suspend fun getAllOccurrences(): List<TaskOccurrenceEntity>

    @Query("SELECT * FROM task_occurrences WHERE id = :occurrenceId LIMIT 1")
    suspend fun getOccurrence(occurrenceId: Long): TaskOccurrenceEntity?

    @Query(
        """
        SELECT o.id AS occurrenceId,
               o.seriesId AS seriesId,
               o.dueEpochDay AS dueEpochDay,
               o.status AS status,
               o.completedAt AS completedAt,
               s.title AS title,
               s.notes AS notes,
               s.important AS important,
               s.urgent AS urgent,
               s.timeMinutes AS timeMinutes,
               s.repeatType AS repeatType,
               s.weekdaysMask AS weekdaysMask,
               s.anchorEpochDay AS anchorEpochDay
        FROM task_occurrences o
        INNER JOIN task_series s ON s.id = o.seriesId
        WHERE o.status = 'PENDING'
        ORDER BY o.dueEpochDay ASC,
                 CASE WHEN s.timeMinutes IS NULL THEN 1 ELSE 0 END ASC,
                 s.timeMinutes ASC,
                 o.createdAt ASC
        """,
    )
    fun observePending(): Flow<List<TaskRecord>>

    @Query(
        """
        SELECT o.id AS occurrenceId,
               o.seriesId AS seriesId,
               o.dueEpochDay AS dueEpochDay,
               o.status AS status,
               o.completedAt AS completedAt,
               s.title AS title,
               s.notes AS notes,
               s.important AS important,
               s.urgent AS urgent,
               s.timeMinutes AS timeMinutes,
               s.repeatType AS repeatType,
               s.weekdaysMask AS weekdaysMask,
               s.anchorEpochDay AS anchorEpochDay
        FROM task_occurrences o
        INNER JOIN task_series s ON s.id = o.seriesId
        WHERE o.status = 'DONE'
          AND o.completedAt >= :startMillis
          AND o.completedAt < :endMillis
        ORDER BY o.completedAt DESC
        """,
    )
    fun observeCompletedBetween(
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<TaskRecord>>

    @Query(
        """
        SELECT o.id AS occurrenceId,
               o.seriesId AS seriesId,
               o.dueEpochDay AS dueEpochDay,
               o.status AS status,
               o.completedAt AS completedAt,
               s.title AS title,
               s.notes AS notes,
               s.important AS important,
               s.urgent AS urgent,
               s.timeMinutes AS timeMinutes,
               s.repeatType AS repeatType,
               s.weekdaysMask AS weekdaysMask,
               s.anchorEpochDay AS anchorEpochDay
        FROM task_occurrences o
        INNER JOIN task_series s ON s.id = o.seriesId
        WHERE o.dueEpochDay BETWEEN :startEpochDay AND :endEpochDay
          AND o.status != 'SKIPPED'
        ORDER BY o.dueEpochDay ASC,
                 CASE WHEN s.timeMinutes IS NULL THEN 1 ELSE 0 END ASC,
                 s.timeMinutes ASC,
                 o.createdAt ASC
        """,
    )
    fun observeBetween(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<TaskRecord>>

    @Query(
        """
        SELECT o.id AS occurrenceId,
               o.seriesId AS seriesId,
               o.dueEpochDay AS dueEpochDay,
               o.status AS status,
               o.completedAt AS completedAt,
               s.title AS title,
               s.notes AS notes,
               s.important AS important,
               s.urgent AS urgent,
               s.timeMinutes AS timeMinutes,
               s.repeatType AS repeatType,
               s.weekdaysMask AS weekdaysMask,
               s.anchorEpochDay AS anchorEpochDay
        FROM task_occurrences o
        INNER JOIN task_series s ON s.id = o.seriesId
        WHERE o.status = 'PENDING'
        ORDER BY o.dueEpochDay ASC, s.timeMinutes ASC
        """,
    )
    suspend fun getPendingRecords(): List<TaskRecord>

    @Query(
        """
        UPDATE task_occurrences
        SET status = :status, completedAt = :completedAt
        WHERE id = :occurrenceId
        """,
    )
    suspend fun setOccurrenceStatus(
        occurrenceId: Long,
        status: OccurrenceStatus,
        completedAt: Long?,
    )

    @Query("UPDATE task_occurrences SET dueEpochDay = :epochDay WHERE id = :occurrenceId")
    suspend fun setOccurrenceDueDate(occurrenceId: Long, epochDay: Long)

    @Query("UPDATE task_series SET important = :important, urgent = :urgent, updatedAt = :updatedAt WHERE id = :seriesId")
    suspend fun setQuadrant(seriesId: String, important: Boolean, urgent: Boolean, updatedAt: Long)

    @Query("UPDATE task_series SET repeatType = :repeatType, updatedAt = :updatedAt WHERE id = :seriesId")
    suspend fun setRepeatType(seriesId: String, repeatType: String, updatedAt: Long)

    @Query("DELETE FROM task_series WHERE id = :seriesId")
    suspend fun deleteSeries(seriesId: String)

    @Query("DELETE FROM task_occurrences WHERE id = :occurrenceId")
    suspend fun deleteOccurrence(occurrenceId: Long)

    @Query("DELETE FROM task_occurrences")
    suspend fun deleteAllOccurrences()

    @Query("DELETE FROM task_series")
    suspend fun deleteAllSeries()
}
