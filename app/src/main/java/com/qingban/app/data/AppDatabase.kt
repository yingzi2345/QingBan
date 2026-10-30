package com.qingban.app.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [
        TaskSeriesEntity::class,
        TaskOccurrenceEntity::class,
        EventEntity::class,
        CountdownEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun countdownDao(): CountdownDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `events` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `startEpochDay` INTEGER NOT NULL,
                        `startMinutes` INTEGER,
                        `reminderMinutes` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_startEpochDay` ON `events` (`startEpochDay`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `countdowns` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `targetEpochDay` INTEGER NOT NULL,
                        `pinned` INTEGER NOT NULL,
                        `yearly` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_countdowns_targetEpochDay` ON `countdowns` (`targetEpochDay`)",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qingban.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
