package com.tvtracker.uptimetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tvtracker.uptimetracker.worker.EmailReportWorker

/**
 * Broadcast receiver that monitors network state changes
 * This is a fallback mechanism for older Android versions
 * Modern versions should use NetworkMonitor instead
 */
class NetworkStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NetworkStateReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            
            if (networkInfo != null && networkInfo.isConnected) {
                Log.i(TAG, "Network connected, scheduling email report")
                
                // Schedule email report worker
                val emailReportRequest = OneTimeWorkRequestBuilder<EmailReportWorker>()
                    .build()
                
                WorkManager.getInstance(context)
                    .enqueue(emailReportRequest)
            }
        }
    }
}
