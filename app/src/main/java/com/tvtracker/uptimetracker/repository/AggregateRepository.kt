package com.tvtracker.uptimetracker.repository

import com.tvtracker.uptimetracker.database.DailyAggregateDao
import com.tvtracker.uptimetracker.model.DailyAggregate
import com.tvtracker.uptimetracker.model.UptimeEvent
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Repository for daily aggregates
 */
class AggregateRepository(private val dailyAggregateDao: DailyAggregateDao) {
    
    /**
     * Get all daily aggregates as a Flow
     */
    fun getAllAggregates(): Flow<List<DailyAggregate>> {
        return dailyAggregateDao.getAllAggregates()
    }
    
    /**
     * Get daily aggregates for a specific day
     */
    suspend fun getAggregatesForDay(date: Date): List<DailyAggregate> {
        return dailyAggregateDao.getAggregatesForDay(date)
    }
    
    /**
     * Get unsent daily aggregates
     */
    suspend fun getUnsentAggregates(): List<DailyAggregate> {
        return dailyAggregateDao.getUnsentAggregates()
    }
    
    /**
     * Mark aggregates as sent
     */
    suspend fun markAggregatesAsSent(aggregateIds: List<Long>) {
        dailyAggregateDao.markAggregatesAsSent(aggregateIds)
    }
    
    /**
     * Delete aggregates older than a week
     */
    suspend fun deleteAggregatesOlderThanOneWeek(): Int {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        return dailyAggregateDao.deleteAggregatesOlderThan(calendar.time)
    }
    
    /**
     * Get aggregates for CSV report
     */
    suspend fun getAggregatesForReport(startDate: Date, endDate: Date): List<DailyAggregate> {
        return dailyAggregateDao.getAggregatesForReport(startDate, endDate)
    }
    
    /**
     * Process raw uptime events into daily aggregates
     * This is the core business logic that converts 5-minute interval data
     * into continuous uptime periods
     */
    suspend fun processEventsIntoAggregates(events: List<UptimeEvent>): List<Long> {
        if (events.isEmpty()) {
            return emptyList()
        }
        
        // Sort events by timestamp
        val sortedEvents = events.sortedBy { it.timestamp }
        
        // Group events by screen name
        val eventsByScreen = sortedEvents.groupBy { it.screenName }
        
        val aggregates = mutableListOf<DailyAggregate>()
        
        // Process each screen separately
        eventsByScreen.forEach { (screenName, screenEvents) ->
            // Find continuous periods
            val periods = findContinuousPeriods(screenEvents)
            
            // Convert periods to aggregates
            periods.forEach { (startTime, endTime) ->
                val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(
                    endTime.time - startTime.time
                ).toInt()
                
                // Only add if duration is at least 5 minutes
                if (durationMinutes >= 5) {
                    aggregates.add(
                        DailyAggregate(
                            screenName = screenName,
                            startTime = startTime,
                            endTime = endTime,
                            totalUptimeMinutes = durationMinutes
                        )
                    )
                }
            }
        }
        
        // Insert all aggregates and return their IDs
        return dailyAggregateDao.insertAll(aggregates)
    }
    
    /**
     * Find continuous periods from a list of events
     * A continuous period is defined as events that are no more than 10 minutes apart
     * (allowing for some missed events)
     */
    private fun findContinuousPeriods(events: List<UptimeEvent>): List<Pair<Date, Date>> {
        if (events.isEmpty()) {
            return emptyList()
        }
        
        val periods = mutableListOf<Pair<Date, Date>>()
        var currentStart = events.first().timestamp
        var lastTimestamp = currentStart
        
        for (i in 1 until events.size) {
            val currentEvent = events[i]
            val timeDiff = currentEvent.timestamp.time - lastTimestamp.time
            
            // If gap is more than 10 minutes, consider it a new period
            if (timeDiff > TimeUnit.MINUTES.toMillis(10)) {
                periods.add(Pair(currentStart, lastTimestamp))
                currentStart = currentEvent.timestamp
            }
            
            lastTimestamp = currentEvent.timestamp
        }
        
        // Add the last period
        periods.add(Pair(currentStart, lastTimestamp))
        
        return periods
    }
}
