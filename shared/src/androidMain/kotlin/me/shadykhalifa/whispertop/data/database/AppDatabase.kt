package me.shadykhalifa.whispertop.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao
import me.shadykhalifa.whispertop.data.database.dao.UserStatisticsDao
import me.shadykhalifa.whispertop.data.database.dao.SessionMetricsDao
import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity
import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity
import me.shadykhalifa.whispertop.data.database.entities.SessionMetricsEntity

@Database(
    entities = [
        TranscriptionHistoryEntity::class,
        UserStatisticsEntity::class,
        SessionMetricsEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun transcriptionHistoryDao(): TranscriptionHistoryDao
    abstract fun userStatisticsDao(): UserStatisticsDao
    abstract fun sessionMetricsDao(): SessionMetricsDao
    
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
                // Create session_metrics table
                database.execSQL("""
                    CREATE TABLE session_metrics (
                        sessionId TEXT PRIMARY KEY NOT NULL,
                        sessionStartTime INTEGER NOT NULL,
                        sessionEndTime INTEGER,
                        audioRecordingDuration INTEGER NOT NULL DEFAULT 0,
                        audioFileSize INTEGER NOT NULL DEFAULT 0,
                        audioQuality TEXT,
                        wordCount INTEGER NOT NULL DEFAULT 0,
                        characterCount INTEGER NOT NULL DEFAULT 0,
                        speakingRate REAL NOT NULL DEFAULT 0.0,
                        transcriptionText TEXT,
                        transcriptionSuccess INTEGER NOT NULL DEFAULT 0,
                        textInsertionSuccess INTEGER NOT NULL DEFAULT 0,
                        targetAppPackage TEXT,
                        errorType TEXT,
                        errorMessage TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indices
                database.execSQL("CREATE INDEX idx_session_start_time ON session_metrics(sessionStartTime)")
                database.execSQL("CREATE INDEX idx_target_app ON session_metrics(targetAppPackage)")
                database.execSQL("CREATE INDEX idx_transcription_success ON session_metrics(transcriptionSuccess)")
            }
        }
        
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add performance-optimized indexes for statistics queries
                
                // Composite index for daily aggregation queries (most common pattern)
                database.execSQL("""
                    CREATE INDEX idx_session_metrics_daily_stats 
                    ON session_metrics(sessionStartTime, transcriptionSuccess, wordCount, characterCount, audioRecordingDuration)
                """.trimIndent())
                
                // Index for error analysis and reporting
                database.execSQL("""
                    CREATE INDEX idx_session_metrics_errors 
                    ON session_metrics(errorType, sessionStartTime) 
                    WHERE errorType IS NOT NULL
                """.trimIndent())
                
                // Index for per-app usage analytics  
                database.execSQL("""
                    CREATE INDEX idx_session_metrics_app_usage 
                    ON session_metrics(targetAppPackage, sessionStartTime, transcriptionSuccess) 
                    WHERE targetAppPackage IS NOT NULL
                """.trimIndent())
                
                // Index for text search on transcriptions (for future search functionality)
                database.execSQL("""
                    CREATE INDEX idx_session_metrics_text_search 
                    ON session_metrics(transcriptionText) 
                    WHERE transcriptionText IS NOT NULL AND LENGTH(transcriptionText) > 0
                """.trimIndent())
                
                // Index for cleanup operations based on file size
                database.execSQL("""
                    CREATE INDEX idx_session_metrics_cleanup 
                    ON session_metrics(sessionStartTime, audioFileSize, LENGTH(transcriptionText))
                """.trimIndent())
                
                // Index for productivity analytics (speaking rate and duration patterns)
                database.execSQL("""
                    CREATE INDEX idx_session_metrics_productivity 
                    ON session_metrics(sessionStartTime, speakingRate, audioRecordingDuration) 
                    WHERE speakingRate > 0
                """.trimIndent())
                
                // Enhance transcription_history indexes if table exists
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_transcription_history_timestamp 
                    ON transcription_history(timestamp)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_transcription_history_success 
                    ON transcription_history(success, timestamp)
                """.trimIndent())
                
                // Enhance user_statistics indexes if table exists
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_user_statistics_date 
                    ON user_statistics(date)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_user_statistics_updated 
                    ON user_statistics(lastUpdated)
                """.trimIndent())
            }
        }
    }
}