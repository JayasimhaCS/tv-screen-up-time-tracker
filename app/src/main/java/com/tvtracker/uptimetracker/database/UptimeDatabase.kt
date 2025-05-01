package com.tvtracker.uptimetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tvtracker.uptimetracker.model.DailyAggregate
import com.tvtracker.uptimetracker.model.EmailLog
import com.tvtracker.uptimetracker.model.UptimeEvent
import com.tvtracker.uptimetracker.util.DateConverter

/**
 * Main database class for the application
 */
@Database(
    entities = [UptimeEvent::class, DailyAggregate::class, EmailLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class UptimeDatabase : RoomDatabase() {
    
    /**
     * Get the UptimeEventDao
     */
    abstract fun uptimeEventDao(): UptimeEventDao
    
    /**
     * Get the DailyAggregateDao
     */
    abstract fun dailyAggregateDao(): DailyAggregateDao
    
    /**
     * Get the EmailLogDao
     */
    abstract fun emailLogDao(): EmailLogDao
    
    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: UptimeDatabase? = null
        
        /**
         * Get the database instance
         */
        fun getDatabase(context: Context): UptimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UptimeDatabase::class.java,
                    "uptime_database"
                )
                .fallbackToDestructiveMigration() // For simplicity in development
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
