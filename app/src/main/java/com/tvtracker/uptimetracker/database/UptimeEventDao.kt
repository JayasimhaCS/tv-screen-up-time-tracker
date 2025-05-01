package com.tvtracker.uptimetracker.database

import androidx.room.*
import com.tvtracker.uptimetracker.model.UptimeEvent
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for UptimeEvent entity
 */
@Dao
interface UptimeEventDao {
    
    /**
     * Insert a new uptime event
     */
    @Insert
    suspend fun insert(event: UptimeEvent): Long
    
    /**
     * Insert multiple uptime events
     */
    @Insert
    suspend fun insertAll(events: List<UptimeEvent>): List<Long>
    
    /**
     * Update an existing uptime event
     */
    @Update
    suspend fun update(event: UptimeEvent)
    
    /**
     * Delete an uptime event
     */
    @Delete
    suspend fun delete(event: UptimeEvent)
    
    /**
     * Get all uptime events
     */
    @Query("SELECT * FROM uptime_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<UptimeEvent>>
    
    /**
     * Get uptime events for a specific day
     */
    @Query("SELECT * FROM uptime_events WHERE timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp ASC")
    suspend fun getEventsForDay(startOfDay: Date, endOfDay: Date): List<UptimeEvent>
    
    /**
     * Get unprocessed uptime events
     */
    @Query("SELECT * FROM uptime_events WHERE processed = 0 ORDER BY timestamp ASC")
    suspend fun getUnprocessedEvents(): List<UptimeEvent>
    
    /**
     * Mark events as processed
     */
    @Query("UPDATE uptime_events SET processed = 1 WHERE id IN (:eventIds)")
    suspend fun markEventsAsProcessed(eventIds: List<Long>)
    
    /**
     * Delete events older than a specific date
     */
    @Query("DELETE FROM uptime_events WHERE timestamp < :date")
    suspend fun deleteEventsOlderThan(date: Date): Int
    
    /**
     * Get the most recent event
     */
    @Query("SELECT * FROM uptime_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentEvent(): UptimeEvent?
}
