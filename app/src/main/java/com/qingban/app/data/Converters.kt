package com.qingban.app.data

import androidx.room.TypeConverter
import com.qingban.app.domain.OccurrenceStatus
import com.qingban.app.domain.RepeatType

class Converters {
    @TypeConverter
    fun repeatTypeToString(value: RepeatType): String = value.name

    @TypeConverter
    fun stringToRepeatType(value: String): RepeatType =
        runCatching { RepeatType.valueOf(value) }.getOrDefault(RepeatType.NONE)

    @TypeConverter
    fun statusToString(value: OccurrenceStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): OccurrenceStatus =
        runCatching { OccurrenceStatus.valueOf(value) }.getOrDefault(OccurrenceStatus.PENDING)
}
