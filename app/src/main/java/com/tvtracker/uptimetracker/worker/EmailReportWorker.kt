package com.tvtracker.uptimetracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvtracker.uptimetracker.database.UptimeDatabase
import com.tvtracker.uptimetracker.repository.AggregateRepository
import com.tvtracker.uptimetracker.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

/**
 * Worker that sends email reports when network connectivity is available
 */
class EmailReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "email_report_worker"
    }
    
    private val database = UptimeDatabase.getDatabase(applicationContext)
    private val aggregateRepository = AggregateRepository(database.dailyAggregateDao())
    private val emailRepository = EmailRepository(applicationContext, database.emailLogDao())
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get unsent aggregates
            val unsentAggregates = aggregateRepository.getUnsentAggregates()
            
            if (unsentAggregates.isNotEmpty()) {
                // Send email report
                val success = emailRepository.sendEmailReport(unsentAggregates)
                
                if (success) {
                    // Mark aggregates as sent
                    val aggregateIds = unsentAggregates.map { it.id }
                    aggregateRepository.markAggregatesAsSent(aggregateIds)
                    
                    // Log success
                    android.util.Log.i(
                        "EmailReportWorker",
                        "Successfully sent email report with ${unsentAggregates.size} records"
                    )
                    
                    Result.success()
                } else {
                    // Log failure and retry
                    android.util.Log.w(
                        "EmailReportWorker",
                        "Failed to send email report, will retry later"
                    )
                    
                    Result.retry()
                }
            } else {
                // No unsent aggregates, nothing to do
                Result.success()
            }
        } catch (e: Exception) {
            // If there's an error, retry the work
            android.util.Log.e(
                "EmailReportWorker",
                "Error sending email report: ${e.message}",
                e
            )
            
            Result.retry()
        }
    }
    
    /**
     * Get the date range for the report (last 24 hours)
     */
    private fun getReportDateRange(): Pair<Date, Date> {
        val endDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = endDate
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startDate = calendar.time
        
        return Pair(startDate, endDate)
    }
}
