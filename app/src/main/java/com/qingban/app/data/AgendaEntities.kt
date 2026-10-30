package com.qingban.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [Index("startEpochDay")],
)
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String,
    val startEpochDay: Long,
    val startMinutes: Int?,
    /** Minutes before the event start; null means no reminder. */
    val reminderMinutes: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "countdowns",
    indices = [Index("targetEpochDay")],
)
data class CountdownEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String,
    val targetEpochDay: Long,
    val pinned: Boolean,
    val yearly: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
