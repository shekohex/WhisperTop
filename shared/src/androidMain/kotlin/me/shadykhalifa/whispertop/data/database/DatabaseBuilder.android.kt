package me.shadykhalifa.whispertop.data.database

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import me.shadykhalifa.whispertop.data.local.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.SQLiteConnection

fun createDatabaseBuilder(context: Context): AppDatabase {
    val tag = "DatabaseBuilder"
    
    return try {
        Log.d(tag, "Initializing encrypted database with SQLCipher")
        
        // Load SQLCipher native library with error handling
        try {
            System.loadLibrary("sqlcipher")
            Log.d(tag, "SQLCipher native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(tag, "Failed to load SQLCipher native library", e)
            throw SecurityException("SQLCipher library not available", e)
        }
        
        // Initialize secure database key management using singleton
        val keyManager = DatabaseKeyManager.getInstance(context)
        var databaseKey: ByteArray? = null
        
        try {
            databaseKey = keyManager.getOrCreateDatabaseKey()
            Log.d(tag, "Database encryption key obtained successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to obtain database key", e)
            throw SecurityException("Database key management failed", e)
        }
        
        // Create SQLCipher hook for security and performance optimizations
        val hook = object : SQLiteDatabaseHook {
            override fun preKey(connection: SQLiteConnection?) {
                // Pre-key operations - could add additional security checks here
                Log.v(tag, "Applying pre-key configurations")
            }
            
            override fun postKey(connection: SQLiteConnection?) {
                try {
                    Log.v(tag, "Applying post-key security configurations")
                    
                    // Conditional memory security: OFF in debug to prevent mlock warnings, ON in production for security
                    val isDebugBuild = try {
                        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to determine debug status for memory security config", e)
                        false
                    }
                    
                    if (isDebugBuild) {
                        Log.v(tag, "DEBUG BUILD: Disabling cipher_memory_security to prevent mlock warnings")
                        connection?.execute("PRAGMA cipher_memory_security = OFF", null, null)
                    } else {
                        Log.v(tag, "PRODUCTION BUILD: Enabling cipher_memory_security for enhanced security")
                        connection?.execute("PRAGMA cipher_memory_security = ON", null, null)
                    }
                    
                    // Removed cipher_plaintext_header_size for security (as per code review)
                    connection?.execute("PRAGMA cipher_cache_size = 16000", null, null) // Larger cache for encrypted DB
                } catch (e: Exception) {
                    Log.e(tag, "Failed to apply SQLCipher security settings", e)
                }
            }
        }
        
        // Create Room database with production-safe configuration
        val builder = Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = AppDatabase.DATABASE_NAME
        )
        .openHelperFactory(SupportOpenHelperFactory(databaseKey, hook, true)) // clearPassphrase=true for security
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
        
        // Apply database callbacks for optimization and monitoring
        builder.addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.i(tag, "Creating new encrypted database")
                
                val startTime = System.currentTimeMillis()
                try {
                    // Enable WAL mode for better concurrent access
                    db.execSQL("PRAGMA journal_mode=WAL")
                    db.execSQL("PRAGMA synchronous=NORMAL")
                    
                    // Performance optimizations for encrypted database
                    db.execSQL("PRAGMA cache_size=16000") // Larger cache for encryption overhead
                    db.execSQL("PRAGMA temp_store=MEMORY")
                    db.execSQL("PRAGMA mmap_size=268435456") // 256MB memory mapping
                    db.execSQL("PRAGMA page_size=4096")
                    
                    // Connection and query settings
                    db.execSQL("PRAGMA busy_timeout=30000") // 30 second busy timeout
                    db.execSQL("PRAGMA foreign_keys=ON") // Enable foreign key constraints
                    
                    val setupTime = System.currentTimeMillis() - startTime
                    Log.d(tag, "Database performance optimizations applied in ${setupTime}ms")
                    
                    // Log database configuration for performance analysis
                    logDatabaseConfiguration(db, tag)
                    
                } catch (e: Exception) {
                    Log.e(tag, "Failed to apply database optimizations", e)
                    // Don't fail database creation for optimization errors
                }
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                val startTime = System.currentTimeMillis()
                
                try {
                    // Re-apply critical security settings on every connection
                    // Use same debug/production logic for consistency
                    val isDebugBuild = try {
                        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to determine debug status for onOpen memory security config", e)
                        false
                    }
                    
                    if (isDebugBuild) {
                        db.execSQL("PRAGMA cipher_memory_security=OFF")
                    } else {
                        db.execSQL("PRAGMA cipher_memory_security=ON")
                    }
                    
                    db.execSQL("PRAGMA foreign_keys=ON")
                    
                    val reopenTime = System.currentTimeMillis() - startTime
                    Log.v(tag, "Database security settings reapplied in ${reopenTime}ms")
                    
                    // Track connection performance
                    if (reopenTime > 50) {
                        Log.w(tag, "Slow database connection: ${reopenTime}ms")
                    }
                    
                } catch (e: Exception) {
                    Log.e(tag, "Failed to reapply security settings", e)
                }
            }
        })
        
        // PRODUCTION SAFETY: Only allow destructive migration in debug builds
        val isDebugBuild = try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            Log.w(tag, "Failed to determine debug status", e)
            false
        }
        
        if (isDebugBuild) {
            Log.w(tag, "DEBUG BUILD: Enabling destructive migration fallback")
            builder.fallbackToDestructiveMigration(true)
        } else {
            Log.i(tag, "PRODUCTION BUILD: Destructive migration disabled for data safety")
            // In production, we must handle migrations properly to avoid data loss
        }
        
        val database = builder.build()
        
        // Clear sensitive data from memory
        databaseKey?.let { DatabaseKeyManager.clearSensitiveData(it) }
        
        Log.i(tag, "Encrypted database initialized successfully")
        database
        
    } catch (e: Exception) {
        Log.e(tag, "Critical failure in database initialization", e)
        throw SecurityException("Encrypted database initialization failed", e)
    }
}

/**
 * Log database configuration for performance analysis
 */
private fun logDatabaseConfiguration(db: SupportSQLiteDatabase, tag: String) {
    try {
        val cursor = db.query("PRAGMA cipher_version")
        cursor.use {
            if (it.moveToFirst()) {
                val cipherVersion = it.getString(0)
                Log.i(tag, "SQLCipher version: $cipherVersion")
            }
        }
        
        // Log other relevant PRAGMA settings
        val pragmas = listOf(
            "journal_mode", "synchronous", "cache_size", 
            "temp_store", "page_size", "foreign_keys"
        )
        
        pragmas.forEach { pragma ->
            try {
                val pragmaCursor = db.query("PRAGMA $pragma")
                pragmaCursor.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val value = cursor.getString(0)
                        Log.d(tag, "PRAGMA $pragma: $value")
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to query PRAGMA $pragma", e)
            }
        }
    } catch (e: Exception) {
        Log.w(tag, "Failed to log database configuration", e)
    }
}