package com.tvtracker.uptimetracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvtracker.uptimetracker.database.UptimeDatabase
import com.tvtracker.uptimetracker.repository.AggregateRepository
import com.tvtracker.uptimetracker.repository.EmailRepository
import com.tvtracker.uptimetracker.repository.UptimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker that cleans up data older than one week
 * This runs once per week
 */
class WeeklyCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "weekly_cleanup_worker"
    }
    
    private val database = UptimeDatabase.getDatabase(applicationContext)
    private val uptimeRepository = UptimeRepository(database.uptimeEventDao())
    private val aggregateRepository = AggregateRepository(database.dailyAggregateDao())
    private val emailRepository = EmailRepository(applicationContext, database.emailLogDao())
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Delete uptime events older than one week
            val deletedEvents = uptimeRepository.deleteEventsOlderThanOneWeek()
            
            // Delete daily aggregates older than one week
            val deletedAggregates = aggregateRepository.deleteAggregatesOlderThanOneWeek()
            
            // Delete email logs older than one week
            val deletedEmailLogs = emailRepository.deleteEmailLogsOlderThanOneWeek()
            
            // Log the cleanup results (in a real app, you might want to log this to a file or analytics)
            android.util.Log.i(
                "WeeklyCleanupWorker",
                "Cleanup completed: Deleted $deletedEvents events, $deletedAggregates aggregates, $deletedEmailLogs email logs"
            )
            
            Result.success()
        } catch (e: Exception) {
            // If there's an error, retry the work
            Result.retry()
        }
    }
}
