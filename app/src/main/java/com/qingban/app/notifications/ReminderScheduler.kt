package com.qingban.app.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.qingban.app.data.AppDatabase
import com.qingban.app.domain.CountdownCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/**
 * Keeps one future alarm per pending occurrence, event and countdown. Every call
 * rebuilds the set, which also removes alarms for tasks that were completed or deleted.
 */
class ReminderScheduler(
    private val context: Context,
    private val database: AppDatabase,
) {
    private val prefs = context.getSharedPreferences("qingban_reminders", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    suspend fun refresh() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val specs = buildList {
                addAll(taskReminders(now, zone))
                addAll(eventReminders(now, zone))
                addAll(countdownReminders(now, zone))
            }
                .distinctBy { it.key }
                .filter { it.triggerAt > now }
                .sortedBy { it.triggerAt }
                .take(MAX_ALARMS)

            val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().toSet()
            val current = specs.mapTo(mutableSetOf()) { it.key }
            (previous - current).forEach(::cancel)

            specs.forEach(::schedule)
            prefs.edit().putStringSet(KEY_SCHEDULED, current).apply()
        }
    }

    private suspend fun taskReminders(now: Long, zone: ZoneId): List<Reminder> =
        database.taskDao().getPendingRecords().mapNotNull { record ->
            val minutes = record.timeMinutes ?: DEFAULT_HOUR * 60
            val trigger = LocalDate.ofEpochDay(record.dueEpochDay)
                .atTime(minutes / 60, minutes % 60)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            if (trigger <= now) return@mapNotNull null
            Reminder(
                key = "task:${record.occurrenceId}",
                triggerAt = trigger,
                title = record.title,
                body = if (record.timeMinutes == null) {
                    "今天记得做这件事"
                } else {
                    "待办提醒 · ${formatMinutes(minutes)}"
                },
            )
        }

    private suspend fun eventReminders(now: Long, zone: ZoneId): List<Reminder> =
        database.eventDao().getAll().mapNotNull { event ->
            val minutes = event.startMinutes ?: DEFAULT_HOUR * 60
            val offset = event.reminderMinutes ?: return@mapNotNull null
            val trigger = LocalDate.ofEpochDay(event.startEpochDay)
                .atTime(minutes / 60, minutes % 60)
                .atZone(zone)
                .toInstant()
                .toEpochMilli() - offset * 60_000L
            if (trigger <= now) return@mapNotNull null
            val timeLabel = if (event.startMinutes == null) "全天" else formatMinutes(minutes)
            Reminder(
                key = "event:${event.id}",
                triggerAt = trigger,
                title = event.title,
                body = "日程提醒 · $timeLabel",
            )
        }

    private suspend fun countdownReminders(now: Long, zone: ZoneId): List<Reminder> {
        val today = LocalDate.now(zone)
        return database.countdownDao().getAll().mapNotNull { countdown ->
            val target = CountdownCalculator.nextOccurrence(
                target = LocalDate.ofEpochDay(countdown.targetEpochDay),
                yearly = countdown.yearly,
                today = today,
            )
            val trigger = target.atTime(DEFAULT_HOUR, 0)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            if (trigger <= now) return@mapNotNull null
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, target)
            val body = when (days) {
                0L -> "就是今天"
                1L -> "还有 1 天"
                else -> "还有 $days 天"
            }
            Reminder(
                key = "countdown:${countdown.id}",
                triggerAt = trigger,
                title = countdown.title,
                body = "倒数日 · $body",
            )
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun schedule(reminder: Reminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntent(reminder.key, reminder.title, reminder.body)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        runCatching {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAt,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAt,
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
        const val KEY_SCHEDULED = "scheduled_keys"
        const val MAX_ALARMS = 200
        const val DEFAULT_HOUR = 9
    }
}
