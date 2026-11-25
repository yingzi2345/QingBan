package com.qingban.app.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.qingban.app.data.AppDatabase
import com.qingban.app.domain.ReminderTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Keeps one future alarm per pending occurrence, event and countdown. Every call
 * rebuilds the set, which also removes alarms for tasks that were completed or deleted.
 *
 * Reminders that are only slightly overdue are kept and re-armed with a small delay:
 * an inexact alarm can be delivered late under Doze, and dropping it at the next
 * refresh would make the reminder disappear entirely.
 */
class ReminderScheduler(
    private val context: Context,
    private val database: AppDatabase,
) {
    private val mutex = Mutex()

    /** True when exact alarms can be used on this device right now. */
    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    suspend fun refresh() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val fired = ReminderState.firedKeys(context)

            val specs = buildList {
                addAll(taskReminders(now, today, zone))
                addAll(eventReminders(now, today, zone))
                addAll(countdownReminders(now, today, zone))
            }
                .distinctBy { it.key }
                .filter { it.key !in fired }
                .filter { ReminderTimeCalculator.isRelevant(it.triggerAt, now) }
                .map { it.copy(triggerAt = ReminderTimeCalculator.effectiveTriggerAt(it.triggerAt, now)) }
                .sortedBy { it.triggerAt }
                .take(MAX_ALARMS)

            val previous = ReminderState.scheduledKeys(context)
            val current = specs.mapTo(mutableSetOf()) { it.key }
            (previous - current).forEach(::cancel)

            specs.forEach(::schedule)
            ReminderState.setScheduledKeys(context, current)
        }
    }

    /** Called by the receiver after a notification is actually shown. */
    fun markFired(key: String) {
        ReminderState.markFired(context, key)
    }

    private suspend fun taskReminders(now: Long, today: LocalDate, zone: ZoneId): List<Reminder> =
        database.taskDao().getPendingRecords().mapNotNull { record ->
            val trigger = ReminderTimeCalculator.taskTriggerMillis(
                dueEpochDay = record.dueEpochDay,
                timeMinutes = record.timeMinutes,
                zone = zone,
            )
            if (!ReminderTimeCalculator.isRelevant(trigger, now)) return@mapNotNull null
            Reminder(
                key = "task:${record.occurrenceId}",
                triggerAt = trigger,
                title = record.title,
                body = if (record.timeMinutes == null) {
                    "今天记得做这件事"
                } else {
                    "待办提醒 · ${formatMinutes(record.timeMinutes!!)}"
                },
            )
        }

    private suspend fun eventReminders(now: Long, today: LocalDate, zone: ZoneId): List<Reminder> =
        database.eventDao().getAll().mapNotNull { event ->
            val trigger = ReminderTimeCalculator.eventTriggerMillis(
                startEpochDay = event.startEpochDay,
                startMinutes = event.startMinutes,
                reminderMinutes = event.reminderMinutes,
                zone = zone,
            ) ?: return@mapNotNull null
            if (!ReminderTimeCalculator.isRelevant(trigger, now)) return@mapNotNull null
            val timeLabel = if (event.startMinutes == null) "全天" else formatMinutes(event.startMinutes!!)
            Reminder(
                key = "event:${event.id}",
                triggerAt = trigger,
                title = event.title,
                body = "日程提醒 · $timeLabel",
            )
        }

    private suspend fun countdownReminders(now: Long, today: LocalDate, zone: ZoneId): List<Reminder> =
        database.countdownDao().getAll().mapNotNull { countdown ->
            val trigger = ReminderTimeCalculator.countdownTriggerMillis(
                target = LocalDate.ofEpochDay(countdown.targetEpochDay),
                yearly = countdown.yearly,
                today = today,
                zone = zone,
            )
            if (!ReminderTimeCalculator.isRelevant(trigger, now)) return@mapNotNull null
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(countdown.targetEpochDay))
            val body = when {
                days < 0 -> "倒数日 · 就是今天"
                days == 1L -> "倒数日 · 还有 1 天"
                else -> "倒数日 · 还有 $days 天"
            }
            Reminder(
                key = "countdown:${countdown.id}",
                triggerAt = trigger,
                title = countdown.title,
                body = body,
            )
        }

    @SuppressLint("ScheduleExactAlarm")
    private fun schedule(reminder: Reminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntent(reminder.key, reminder.title, reminder.body)
        val exact = canScheduleExactAlarms()
        runCatching {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAt,
                    pendingIntent,
                )
            } else {
                // Inexact alarms may be delayed up to an hour under Doze on Android 12+.
                // setWindow gives the system a bounded window while still not requiring
                // the exact-alarm permission. The grace window above keeps them alive.
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAt,
                    WINDOW_MILLIS,
                    pendingIntent,
                )
            }
        }
    }

    private fun cancel(key: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(key))
    }

    private fun pendingIntent(
        key: String,
        title: String? = null,
        body: String? = null,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            reminderIntent(key, title, body),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun reminderIntent(
        key: String,
        title: String? = null,
        body: String? = null,
    ): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            data = Uri.parse("qingban://reminder/${Uri.encode(key)}")
            putExtra(ReminderReceiver.EXTRA_KEY, key)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
        }

    private fun formatMinutes(minutes: Int): String =
        String.format(Locale.CHINA, "%02d:%02d", minutes / 60, minutes % 60)

    private data class Reminder(
        val key: String,
        val triggerAt: Long,
        val title: String,
        val body: String,
    )

    private companion object {
        const val MAX_ALARMS = 200
        const val WINDOW_MILLIS = 60_000L
    }
}
