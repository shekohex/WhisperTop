package me.shadykhalifa.whispertop.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createDatabaseBuilder(): AppDatabase {
    val dbFilePath = documentDirectory() + "/${AppDatabase.DATABASE_NAME}"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
    )
    .addMigrations(AppDatabase.MIGRATION_1_2)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
}

private fun documentDirectory(): String {
    // Platform-specific path for iOS documents directory
    return "" // TODO: Implement iOS document directory path
}