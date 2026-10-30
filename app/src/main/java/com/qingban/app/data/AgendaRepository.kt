package com.qingban.app.data

import com.qingban.app.domain.CountdownDraft
import com.qingban.app.domain.CountdownUi
import com.qingban.app.domain.EventDraft
import com.qingban.app.domain.EventUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class AgendaRepository(
    database: AppDatabase,
) {
    private val eventDao = database.eventDao()
    private val countdownDao = database.countdownDao()

    fun observeEventsBetween(start: LocalDate, endInclusive: LocalDate): Flow<List<EventUi>> =
        eventDao.observeBetween(start.toEpochDay(), endInclusive.toEpochDay())
            .map { events -> events.map { it.toUi() } }

    val allEvents: Flow<List<EventUi>> =
        eventDao.observeAll().map { events -> events.map { it.toUi() } }

    val countdowns: Flow<List<CountdownUi>> =
        countdownDao.observeAll().map { items -> items.map { it.toUi() } }

    suspend fun createEvent(draft: EventDraft): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        eventDao.insert(
            EventEntity(
                id = id,
                title = draft.title.trim(),
                notes = draft.notes.trim(),
                startEpochDay = draft.date.toEpochDay(),
                startMinutes = draft.startMinutes,
                reminderMinutes = draft.reminderMinutes,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun updateEvent(draft: EventDraft): String {
        val id = requireNotNull(draft.id) { "Missing event id" }
        val existing = requireNotNull(eventDao.get(id)) { "Event no longer exists" }
        eventDao.update(
            existing.copy(
                title = draft.title.trim(),
                notes = draft.notes.trim(),
                startEpochDay = draft.date.toEpochDay(),
                startMinutes = draft.startMinutes,
                reminderMinutes = draft.reminderMinutes,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    suspend fun deleteEvent(id: String) = eventDao.delete(id)

    suspend fun createCountdown(draft: CountdownDraft): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        countdownDao.insert(
            CountdownEntity(
                id = id,
                title = draft.title.trim(),
                notes = draft.notes.trim(),
                targetEpochDay = draft.targetDate.toEpochDay(),
                pinned = draft.pinned,
                yearly = draft.yearly,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun updateCountdown(draft: CountdownDraft): String {
        val id = requireNotNull(draft.id) { "Missing countdown id" }
        val existing = requireNotNull(countdownDao.get(id)) { "Countdown no longer exists" }
        countdownDao.update(
            existing.copy(
                title = draft.title.trim(),
                notes = draft.notes.trim(),
                targetEpochDay = draft.targetDate.toEpochDay(),
                pinned = draft.pinned,
                yearly = draft.yearly,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    suspend fun setCountdownPinned(id: String, pinned: Boolean) {
        countdownDao.setPinned(id, pinned, System.currentTimeMillis())
    }

    suspend fun deleteCountdown(id: String) = countdownDao.delete(id)

    private fun EventEntity.toUi(): EventUi = EventUi(
        id = id,
        title = title,
        notes = notes,
        date = LocalDate.ofEpochDay(startEpochDay),
        startMinutes = startMinutes,
        reminderMinutes = reminderMinutes,
        createdAt = createdAt,
    )

    private fun CountdownEntity.toUi(): CountdownUi = CountdownUi(
        id = id,
        title = title,
        notes = notes,
        targetDate = LocalDate.ofEpochDay(targetEpochDay),
        pinned = pinned,
        yearly = yearly,
        createdAt = createdAt,
    )
}
