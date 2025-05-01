package com.tvtracker.uptimetracker.worker

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvtracker.uptimetracker.database.UptimeDatabase
import com.tvtracker.uptimetracker.repository.UptimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker that records uptime events every 5 minutes
 */
class UptimeTrackerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "uptime_tracker_worker"
        
        // Event types
        const val EVENT_TYPE_ACTIVE = "ACTIVE"
        const val EVENT_TYPE_IDLE = "IDLE"
        const val EVENT_TYPE_STANDBY = "STANDBY"
    }
    
    private val uptimeRepository: UptimeRepository by lazy {
        val database = UptimeDatabase.getDatabase(applicationContext)
        UptimeRepository(database.uptimeEventDao())
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Determine the current device state
            val eventType = determineDeviceState()
            
            // Record the uptime event
            uptimeRepository.recordUptimeEvent(eventType)
            
            Result.success()
        } catch (e: Exception) {
            // If there's an error, retry the work
            Result.retry()
        }
    }
    
    /**
     * Determine the current device state (active, idle, or standby)
     */
    private fun determineDeviceState(): String {
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // Check if the screen is on
        val isScreenOn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
        
        // Check if the device is idle (Doze mode, API 23+)
        val isDeviceIdle = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isDeviceIdleMode
        } else {
            false
        }
        
        return when {
            !isScreenOn -> EVENT_TYPE_STANDBY
            isDeviceIdle -> EVENT_TYPE_IDLE
            else -> EVENT_TYPE_ACTIVE
        }
    }
}
