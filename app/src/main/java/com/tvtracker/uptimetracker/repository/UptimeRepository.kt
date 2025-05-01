package com.tvtracker.uptimetracker.repository

import android.os.Build
import com.tvtracker.uptimetracker.database.UptimeEventDao
import com.tvtracker.uptimetracker.model.UptimeEvent
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date

/**
 * Repository for uptime events
 */
class UptimeRepository(private val uptimeEventDao: UptimeEventDao) {
    
    /**
     * Get all uptime events as a Flow
     */
    fun getAllEvents(): Flow<List<UptimeEvent>> {
        return uptimeEventDao.getAllEvents()
    }
    
    /**
     * Record a new uptime event
     */
    suspend fun recordUptimeEvent(eventType: String): Long {
        val screenName = getScreenName()
        val event = UptimeEvent(
            screenName = screenName,
            timestamp = Date(),
            eventType = eventType
        )
        return uptimeEventDao.insert(event)
    }
    
    /**
     * Get uptime events for a specific day
     */
    suspend fun getEventsForDay(date: Date): List<UptimeEvent> {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.time
        
        return uptimeEventDao.getEventsForDay(startOfDay, endOfDay)
    }
    
    /**
     * Get unprocessed uptime events
     */
    suspend fun getUnprocessedEvents(): List<UptimeEvent> {
        return uptimeEventDao.getUnprocessedEvents()
    }
    
    /**
     * Mark events as processed
     */
    suspend fun markEventsAsProcessed(eventIds: List<Long>) {
        uptimeEventDao.markEventsAsProcessed(eventIds)
    }
    
    /**
     * Delete events older than a week
     */
    suspend fun deleteEventsOlderThanOneWeek(): Int {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        return uptimeEventDao.deleteEventsOlderThan(calendar.time)
    }
    
    /**
     * Get the most recent event
     */
    suspend fun getMostRecentEvent(): UptimeEvent? {
        return uptimeEventDao.getMostRecentEvent()
    }
    
    /**
     * Get the screen name from the device
     */
    private fun getScreenName(): String {
        // In a real implementation, this would get the screen name from the OS environment
        // For now, we'll use the device model as a placeholder
        return Build.MODEL
    }
}
