package me.shadykhalifa.whispertop.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao
import me.shadykhalifa.whispertop.data.database.dao.UserStatisticsDao
import me.shadykhalifa.whispertop.data.models.TranscriptionHistoryEntity
import me.shadykhalifa.whispertop.data.models.UserStatisticsEntity

@Database(
    entities = [
        TranscriptionHistoryEntity::class,
        UserStatisticsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun transcriptionHistoryDao(): TranscriptionHistoryDao
    abstract fun userStatisticsDao(): UserStatisticsDao
    
    companion object {
        const val DATABASE_NAME = "whispertop_database.db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(builder: Builder<AppDatabase>): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
        
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Future migration logic will go here
            }
        }
    }
}