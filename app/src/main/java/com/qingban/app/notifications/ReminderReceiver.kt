package com.qingban.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "轻办提醒" }
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val id = intent.dataString?.hashCode() ?: title.hashCode()
        NotificationHelper.show(context, title, body, id)
    }

    companion object {
        const val ACTION_REMIND = "com.qingban.app.REMIND"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}
