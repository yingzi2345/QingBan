package com.qingban.app

import android.app.Application
import com.qingban.app.data.AgendaRepository
import com.qingban.app.data.AppDatabase
import com.qingban.app.data.BackupManager
import com.qingban.app.data.SettingsRepository
import com.qingban.app.data.TaskRepository
import com.qingban.app.notifications.NotificationHelper
import com.qingban.app.notifications.ReminderScheduler

class QingBanApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: TaskRepository by lazy {
        TaskRepository(database)
    }

    val agendaRepository: AgendaRepository by lazy {
        AgendaRepository(database)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(database)
    }

    val settings: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(this, database)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
