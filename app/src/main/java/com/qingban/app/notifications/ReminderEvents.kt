package com.qingban.app.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-app delivery channel for reminders that fire while the UI is in the
 * foreground. This complements the system notification so the user still sees
 * the reminder when POST_NOTIFICATIONS has not been granted.
 */
object ReminderEvents {
    private val _events = MutableSharedFlow<ReminderEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ReminderEvent> = _events.asSharedFlow()

    private val recentKeys = mutableMapOf<String, Long>()

    /**
     * Emits at most once per key within [DEDUPE_WINDOW_MS]. An alarm that could
     * not post a notification stays eligible for re-scheduling, but the in-app
     * dialog must not reappear on every resume.
     */
    fun emit(key: String, title: String, body: String) {
        val now = System.currentTimeMillis()
        synchronized(recentKeys) {
            val last = recentKeys[key] ?: 0L
            if (now - last < DEDUPE_WINDOW_MS) return
            recentKeys[key] = now
            if (recentKeys.size > 100) {
                val cutoff = now - DEDUPE_WINDOW_MS
                recentKeys.entries.removeIf { it.value < cutoff }
            }
        }
        _events.tryEmit(ReminderEvent(key, title, body))
    }

    data class ReminderEvent(val key: String, val title: String, val body: String)

    private const val DEDUPE_WINDOW_MS = 60_000L
}
