package dev.dwm.liftlog.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createDatabase(context: Context): AppDatabase =
    Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath("liftlog.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
