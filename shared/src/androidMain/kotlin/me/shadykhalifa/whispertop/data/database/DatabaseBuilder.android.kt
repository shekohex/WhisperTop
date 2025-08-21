package me.shadykhalifa.whispertop.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase

fun createDatabaseBuilder(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context = context,
        klass = AppDatabase::class.java,
        name = AppDatabase.DATABASE_NAME
    )
    .addMigrations(AppDatabase.MIGRATION_1_2)
    .addCallback(object : androidx.room.RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Enable WAL mode for better performance
            db.execSQL("PRAGMA journal_mode=WAL")
            db.execSQL("PRAGMA synchronous=NORMAL")
        }
    })
    .fallbackToDestructiveMigration() // Only for development
    .build()
}