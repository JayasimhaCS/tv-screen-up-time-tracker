package com.tvtracker.uptimetracker

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import com.tvtracker.uptimetracker.service.UptimeTrackerService

/**
 * Application class for initializing components
 */
class UptimeTrackerApplication : Application() {
    
    companion object {
        private const val TAG = "UptimeTrackerApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate")
        
        // Initialize WorkManager
        initializeWorkManager()
        
        // Start the uptime tracker service
        startUptimeTrackerService()
    }
    
    /**
     * Start the uptime tracker service
     */
    private fun startUptimeTrackerService() {
        Log.i(TAG, "Starting UptimeTrackerService")
        
        val serviceIntent = Intent(this, UptimeTrackerService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    /**
     * Initialize WorkManager
     */
    private fun initializeWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
            
        // Initialize WorkManager with the configuration
        WorkManager.initialize(this, config)
    }
}
