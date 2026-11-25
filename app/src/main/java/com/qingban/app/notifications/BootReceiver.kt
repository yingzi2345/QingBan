package com.qingban.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qingban.app.QingBanApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager forgets alarms across reboots, so rebuild them from the database. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pendingResult = goAsync()
        val app = context.applicationContext as? QingBanApplication ?: run {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.reminderScheduler.refresh()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            // Inlined as a string constant; keeps minSdk 26 lint clean.
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
