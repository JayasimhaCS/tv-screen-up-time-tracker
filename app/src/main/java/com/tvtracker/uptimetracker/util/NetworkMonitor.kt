package com.tvtracker.uptimetracker.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tvtracker.uptimetracker.worker.EmailReportWorker

/**
 * Monitors network connectivity and triggers email report when network becomes available
 */
class NetworkMonitor(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Network is available, schedule email report worker
            scheduleEmailReport()
        }
    }
    
    /**
     * Start monitoring network connectivity
     */
    fun startMonitoring() {
        // Check if network is already available
        if (isNetworkAvailable()) {
            scheduleEmailReport()
        }
        
        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    /**
     * Stop monitoring network connectivity
     */
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }
    
    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }
    
    /**
     * Schedule email report worker
     */
    private fun scheduleEmailReport() {
        val emailReportRequest = OneTimeWorkRequestBuilder<EmailReportWorker>()
            .build()
        
        WorkManager.getInstance(context)
            .enqueue(emailReportRequest)
    }
}
