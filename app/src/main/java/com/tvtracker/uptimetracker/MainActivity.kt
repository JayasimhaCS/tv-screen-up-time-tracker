package com.tvtracker.uptimetracker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tvtracker.uptimetracker.database.UptimeDatabase
import com.tvtracker.uptimetracker.repository.AggregateRepository
import com.tvtracker.uptimetracker.repository.EmailRepository
import com.tvtracker.uptimetracker.service.UptimeTrackerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main activity for the application
 * Provides a simple UI for viewing uptime statistics and configuring email settings
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var tvDeviceName: TextView
    private lateinit var tvUptimeToday: TextView
    private lateinit var tvLastReport: TextView
    private lateinit var etEmailAddress: EditText
    private lateinit var etEmailPassword: EditText
    private lateinit var etRecipientEmail: EditText
    private lateinit var btnSaveSettings: Button
    private lateinit var btnStartService: Button
    
    private lateinit var database: UptimeDatabase
    private lateinit var aggregateRepository: AggregateRepository
    private lateinit var emailRepository: EmailRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        tvDeviceName = findViewById(R.id.tv_device_name)
        tvUptimeToday = findViewById(R.id.tv_uptime_today)
        tvLastReport = findViewById(R.id.tv_last_report)
        etEmailAddress = findViewById(R.id.et_email_address)
        etEmailPassword = findViewById(R.id.et_email_password)
        etRecipientEmail = findViewById(R.id.et_recipient_email)
        btnSaveSettings = findViewById(R.id.btn_save_settings)
        btnStartService = findViewById(R.id.btn_start_service)
        
        // Initialize database and repositories
        database = UptimeDatabase.getDatabase(this)
        aggregateRepository = AggregateRepository(database.dailyAggregateDao())
        emailRepository = EmailRepository(this, database.emailLogDao())
        
        // Set device name
        tvDeviceName.text = getString(R.string.device_name, Build.MODEL)
        
        // Set up button click listeners
        btnSaveSettings.setOnClickListener {
            saveEmailSettings()
        }
        
        btnStartService.setOnClickListener {
            startUptimeTrackerService()
        }
        
        // Load uptime data
        loadUptimeData()
        
        // Load email logs
        loadEmailLogs()
    }
    
    /**
     * Load uptime data
     */
    private fun loadUptimeData() {
        lifecycleScope.launch {
            val today = Date()
            val aggregates = withContext(Dispatchers.IO) {
                aggregateRepository.getAggregatesForDay(today)
            }
            
            // Calculate total uptime for today
            val totalMinutes = aggregates.sumOf { it.totalUptimeMinutes }
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            
            tvUptimeToday.text = getString(R.string.uptime_today, hours, minutes)
        }
    }
    
    /**
     * Load email logs
     */
    private fun loadEmailLogs() {
        lifecycleScope.launch {
            emailRepository.getAllEmailLogs().collect { logs ->
                if (logs.isNotEmpty()) {
                    val lastLog = logs.first()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    val status = if (lastLog.success) "Success" else "Failed"
                    
                    tvLastReport.text = getString(R.string.last_report, "${dateFormat.format(lastLog.sentTime)} - $status")
                } else {
                    tvLastReport.text = getString(R.string.last_report_none)
                }
            }
        }
    }
    
    /**
     * Save email settings
     */
    private fun saveEmailSettings() {
        val emailAddress = etEmailAddress.text.toString().trim()
        val password = etEmailPassword.text.toString().trim()
        val recipientEmail = etRecipientEmail.text.toString().trim()
        
        if (emailAddress.isEmpty() || password.isEmpty() || recipientEmail.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }
        
        emailRepository.saveEmailSettings(
            emailAddress = emailAddress,
            password = password,
            recipientEmail = recipientEmail
        )
        
        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        
        // Clear password field for security
        etEmailPassword.setText("")
    }
    
    /**
     * Start the uptime tracker service
     */
    private fun startUptimeTrackerService() {
        val serviceIntent = Intent(this, UptimeTrackerService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        Toast.makeText(this, getString(R.string.service_started), Toast.LENGTH_SHORT).show()
    }
}
