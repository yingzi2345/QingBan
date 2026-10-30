package com.qingban.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.qingban.app.domain.OccurrenceStatus
import com.qingban.app.domain.RepeatType

@Entity(tableName = "task_series")
data class TaskSeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String,
    val important: Boolean,
    val urgent: Boolean,
    val timeMinutes: Int?,
    val repeatType: RepeatType,
    val weekdaysMask: Int,
    val anchorEpochDay: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "task_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = TaskSeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("seriesId"),
        Index("status"),
        Index("completedAt"),
    ],
)
data class TaskOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: String,
    val dueEpochDay: Long,
    val status: OccurrenceStatus,
    val completedAt: Long?,
    val createdAt: Long,
)
