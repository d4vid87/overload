package dev.dwm.liftlog.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

fun createDatabase(): AppDatabase {
    val dir = File(System.getProperty("user.home"), ".liftlog").apply { mkdirs() }
    return Room.databaseBuilder<AppDatabase>(File(dir, "liftlog.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
