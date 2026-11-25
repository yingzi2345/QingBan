package com.qingban.app.notifications

import android.content.Context

/**
 * Tracks which reminder keys have been scheduled and which have already fired.
 * SharedPreferences is used because the receiver runs in a separate process
 * lifecycle from the UI and must survive process death.
 */
internal object ReminderState {
    private const val PREFS = "qingban_reminders"
    private const val KEY_SCHEDULED = "scheduled_keys"
    private const val KEY_FIRED = "fired_keys"
    private const val MAX_FIRED = 300

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun scheduledKeys(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().toSet()

    fun setScheduledKeys(context: Context, keys: Set<String>) {
        prefs(context).edit().putStringSet(KEY_SCHEDULED, HashSet(keys)).apply()
    }

    fun firedKeys(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_FIRED, emptySet()).orEmpty().toSet()

    /** Fired keys are kept bounded so the set cannot grow without limit. */
    fun markFired(context: Context, key: String) {
        val updated = LinkedHashSet(firedKeys(context)).apply { add(key) }
        while (updated.size > MAX_FIRED) {
            updated.iterator().let { iterator ->
                iterator.next()
                iterator.remove()
            }
        }
        prefs(context).edit().putStringSet(KEY_FIRED, HashSet(updated)).apply()
    }

    fun clearAll(context: Context) {
        prefs(context).edit()
            .remove(KEY_SCHEDULED)
            .remove(KEY_FIRED)
            .apply()
    }
}
