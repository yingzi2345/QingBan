package com.qingban.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qingban.app.QingBanApplication

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val key = intent.getStringExtra(EXTRA_KEY).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "轻办提醒" }
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val notificationId = key.hashCode()

        // Always try to show the notification; the helper itself checks the
        // permission state. In the foreground the activity observes the same
        // key through the in-app channel so the user sees it even if the
        // notification permission was denied.
        val shown = NotificationHelper.show(context, title, body, notificationId)
        ReminderEvents.emit(key, title, body)
        if (shown) {
            // Only clear the alarm once the user actually got a notification.
            // If permission is missing we leave it eligible for the next
            // refresh, so granting permission later still delivers it.
            (context.applicationContext as? QingBanApplication)?.reminderScheduler?.markFired(key)
        }
    }

    companion object {
        const val ACTION_REMIND = "com.qingban.app.REMIND"
        const val EXTRA_KEY = "key"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}
