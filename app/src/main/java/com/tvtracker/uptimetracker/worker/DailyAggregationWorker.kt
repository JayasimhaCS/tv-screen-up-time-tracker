package com.tvtracker.uptimetracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvtracker.uptimetracker.database.UptimeDatabase
import com.tvtracker.uptimetracker.repository.AggregateRepository
import com.tvtracker.uptimetracker.repository.UptimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker that aggregates raw uptime events into daily summaries
 * This runs once per day, typically at midnight
 */
class DailyAggregationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "daily_aggregation_worker"
    }
    
    private val database = UptimeDatabase.getDatabase(applicationContext)
    private val uptimeRepository = UptimeRepository(database.uptimeEventDao())
    private val aggregateRepository = AggregateRepository(database.dailyAggregateDao())
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get all unprocessed events
            val unprocessedEvents = uptimeRepository.getUnprocessedEvents()
            
            if (unprocessedEvents.isNotEmpty()) {
                // Process events into aggregates
                val aggregateIds = aggregateRepository.processEventsIntoAggregates(unprocessedEvents)
                
                // Mark events as processed
                if (aggregateIds.isNotEmpty()) {
                    val eventIds = unprocessedEvents.map { it.id }
                    uptimeRepository.markEventsAsProcessed(eventIds)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            // If there's an error, retry the work
            Result.retry()
        }
    }
}
