package com.tvtracker.uptimetracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tvtracker.uptimetracker.MainActivity
import com.tvtracker.uptimetracker.R
import com.tvtracker.uptimetracker.util.NetworkMonitor
import com.tvtracker.uptimetracker.worker.DailyAggregationWorker
import com.tvtracker.uptimetracker.worker.UptimeTrackerWorker
import com.tvtracker.uptimetracker.worker.WeeklyCleanupWorker
import java.util.concurrent.TimeUnit

/**
 * Foreground service that runs continuously to track TV uptime
 */
class UptimeTrackerService : LifecycleService() {
    
    companion object {
        private const val TAG = "UptimeTrackerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "uptime_tracker_channel"
        private const val CHANNEL_NAME = "Uptime Tracker"
        private const val WAKE_LOCK_TAG = "UptimeTracker:WakeLock"
    }
    
    private lateinit var networkMonitor: NetworkMonitor
    private var wakeLock: PowerManager.WakeLock? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate")
        
        // Create notification channel for Android O and above
        createNotificationChannel()
        
        // Start as a foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Initialize network monitor
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        
        // Acquire wake lock to keep CPU running
        acquireWakeLock()
        
        // Schedule workers
        scheduleWorkers()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "Service onStartCommand")
        
        // If service is killed, restart it
        return START_STICKY
    }
    
    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy")
        
        // Release wake lock
        releaseWakeLock()
        
        // Stop network monitoring
        networkMonitor.stopMonitoring()
        
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Acquire wake lock to keep CPU running
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire()
        }
    }
    
    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
    
    /**
     * Schedule all required workers
     */
    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)
        
        // Schedule uptime tracker worker (every 5 minutes)
        val uptimeTrackerRequest = PeriodicWorkRequestBuilder<UptimeTrackerWorker>(
            5, TimeUnit.MINUTES
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            UptimeTrackerWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            uptimeTrackerRequest
        )
        
        // Schedule daily aggregation worker (once per day at midnight)
        val dailyAggregationRequest = PeriodicWorkRequestBuilder<DailyAggregationWorker>(
            24, TimeUnit.HOURS
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            DailyAggregationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyAggregationRequest
        )
        
        // Schedule weekly cleanup worker (once per week)
        val weeklyCleanupRequest = PeriodicWorkRequestBuilder<WeeklyCleanupWorker>(
            7, TimeUnit.DAYS
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            WeeklyCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            weeklyCleanupRequest
        )
        
        // Run initial uptime tracking immediately
        val initialUptimeRequest = OneTimeWorkRequestBuilder<UptimeTrackerWorker>()
            .build()
        
        workManager.enqueueUniqueWork(
            "initial_uptime_tracking",
            ExistingWorkPolicy.REPLACE,
            initialUptimeRequest
        )
    }
}
