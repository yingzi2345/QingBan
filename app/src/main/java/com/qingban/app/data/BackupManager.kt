package com.qingban.app.data

import androidx.room.withTransaction
import com.qingban.app.domain.OccurrenceStatus
import com.qingban.app.domain.RepeatType
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val series: Int,
    val occurrences: Int,
    val events: Int,
    val countdowns: Int,
)

/**
 * Reads and replaces the whole local database. The JSON format is intentionally
 * flat and versioned so a backup stays readable and future versions can migrate it.
 */
class BackupManager(
    private val database: AppDatabase,
) {
    suspend fun exportJson(): String = database.withTransaction {
        val dao = database.taskDao()
        val series = dao.getAllSeries()
        val occurrences = dao.getAllOccurrences()
        val events = database.eventDao().getAll()
        val countdowns = database.countdownDao().getAll()

        val root = JSONObject().apply {
            put("app", APP_ID)
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("taskSeries", JSONArray().apply {
                series.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("notes", item.notes)
                        put("important", item.important)
                        put("urgent", item.urgent)
                        put("timeMinutes", item.timeMinutes ?: JSONObject.NULL)
                        put("repeatType", item.repeatType.name)
                        put("weekdaysMask", item.weekdaysMask)
                        put("anchorEpochDay", item.anchorEpochDay)
                        put("createdAt", item.createdAt)
                        put("updatedAt", item.updatedAt)
                    })
                }
            })
            put("taskOccurrences", JSONArray().apply {
                occurrences.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("seriesId", item.seriesId)
                        put("dueEpochDay", item.dueEpochDay)
                        put("status", item.status.name)
                        put("completedAt", item.completedAt ?: JSONObject.NULL)
                        put("createdAt", item.createdAt)
                    })
                }
            })
            put("events", JSONArray().apply {
                events.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("notes", item.notes)
                        put("startEpochDay", item.startEpochDay)
                        put("startMinutes", item.startMinutes ?: JSONObject.NULL)
                        put("reminderMinutes", item.reminderMinutes ?: JSONObject.NULL)
                        put("createdAt", item.createdAt)
                        put("updatedAt", item.updatedAt)
                    })
                }
            })
            put("countdowns", JSONArray().apply {
                countdowns.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("notes", item.notes)
                        put("targetEpochDay", item.targetEpochDay)
                        put("pinned", item.pinned)
                        put("yearly", item.yearly)
                        put("createdAt", item.createdAt)
                        put("updatedAt", item.updatedAt)
                    })
                }
            })
        }
        root.toString(2)
    }

    suspend fun importJson(raw: String): BackupSummary = database.withTransaction {
        val root = runCatching { JSONObject(raw) }.getOrElse {
            throw IllegalArgumentException("不是有效的 JSON 文件")
        }
        require(root.optString("app") == APP_ID) { "这不是轻办的备份文件" }
        val version = root.optInt("schemaVersion", -1)
        require(version == SCHEMA_VERSION) { "备份版本不受支持（$version）" }

        val seriesArray = root.optJSONArray("taskSeries")
            ?: throw IllegalArgumentException("备份缺少待办数据")
        val occurrenceArray = root.optJSONArray("taskOccurrences")
            ?: JSONArray()
        val eventArray = root.optJSONArray("events") ?: JSONArray()
        val countdownArray = root.optJSONArray("countdowns") ?: JSONArray()

        val series = buildList {
            for (index in 0 until seriesArray.length()) {
                val item = seriesArray.getJSONObject(index)
                val id = item.optString("id")
                require(id.isNotBlank()) { "备份中的待办缺少标识" }
                add(
                    TaskSeriesEntity(
                        id = id,
                        title = item.optString("title"),
                        notes = item.optString("notes"),
                        important = item.optBoolean("important"),
                        urgent = item.optBoolean("urgent"),
                        timeMinutes = item.optNullableInt("timeMinutes"),
                        repeatType = runCatching {
                            RepeatType.valueOf(item.optString("repeatType", RepeatType.NONE.name))
                        }.getOrDefault(RepeatType.NONE),
                        weekdaysMask = item.optInt("weekdaysMask"),
                        anchorEpochDay = item.optLong("anchorEpochDay"),
                        createdAt = item.optLong("createdAt"),
                        updatedAt = item.optLong("updatedAt"),
                    ),
                )
            }
        }
        val seriesIds = series.mapTo(mutableSetOf()) { it.id }
        val occurrences = buildList {
            for (index in 0 until occurrenceArray.length()) {
                val item = occurrenceArray.getJSONObject(index)
                val seriesId = item.optString("seriesId")
                require(seriesId in seriesIds) { "备份中的待办实例没有对应任务" }
                add(
                    TaskOccurrenceEntity(
                        id = item.optLong("id"),
                        seriesId = seriesId,
                        dueEpochDay = item.optLong("dueEpochDay"),
                        status = runCatching {
                            OccurrenceStatus.valueOf(item.optString("status", OccurrenceStatus.PENDING.name))
                        }.getOrDefault(OccurrenceStatus.PENDING),
                        completedAt = item.optNullableLong("completedAt"),
                        createdAt = item.optLong("createdAt"),
                    ),
                )
            }
        }
        val events = buildList {
            for (index in 0 until eventArray.length()) {
                val item = eventArray.getJSONObject(index)
                val id = item.optString("id")
                require(id.isNotBlank()) { "备份中的日程缺少标识" }
                add(
                    EventEntity(
                        id = id,
                        title = item.optString("title"),
                        notes = item.optString("notes"),
                        startEpochDay = item.optLong("startEpochDay"),
                        startMinutes = item.optNullableInt("startMinutes"),
                        reminderMinutes = item.optNullableInt("reminderMinutes"),
                        createdAt = item.optLong("createdAt"),
                        updatedAt = item.optLong("updatedAt"),
                    ),
                )
            }
        }
        val countdowns = buildList {
            for (index in 0 until countdownArray.length()) {
                val item = countdownArray.getJSONObject(index)
                val id = item.optString("id")
                require(id.isNotBlank()) { "备份中的倒数日缺少标识" }
                add(
                    CountdownEntity(
                        id = id,
                        title = item.optString("title"),
                        notes = item.optString("notes"),
                        targetEpochDay = item.optLong("targetEpochDay"),
                        pinned = item.optBoolean("pinned"),
                        yearly = item.optBoolean("yearly"),
                        createdAt = item.optLong("createdAt"),
                        updatedAt = item.optLong("updatedAt"),
                    ),
                )
            }
        }

        val dao = database.taskDao()
        dao.deleteAllOccurrences()
        dao.deleteAllSeries()
        database.eventDao().deleteAll()
        database.countdownDao().deleteAll()

        if (series.isNotEmpty()) dao.insertSeriesList(series)
        if (occurrences.isNotEmpty()) dao.insertOccurrenceList(occurrences)
        if (events.isNotEmpty()) database.eventDao().insertAll(events)
        if (countdowns.isNotEmpty()) database.countdownDao().insertAll(countdowns)

        BackupSummary(
            series = series.size,
            occurrences = occurrences.size,
            events = events.size,
            countdowns = countdowns.size,
        )
    }

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private companion object {
        const val APP_ID = "qingban"
        const val SCHEMA_VERSION = 1
    }
}
