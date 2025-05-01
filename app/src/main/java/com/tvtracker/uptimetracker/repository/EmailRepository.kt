package com.tvtracker.uptimetracker.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.opencsv.CSVWriter
import com.tvtracker.uptimetracker.database.EmailLogDao
import com.tvtracker.uptimetracker.model.DailyAggregate
import com.tvtracker.uptimetracker.model.EmailLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Repository for email operations
 */
class EmailRepository(
    private val context: Context,
    private val emailLogDao: EmailLogDao
) {
    
    companion object {
        private const val PREF_FILE_NAME = "email_settings"
        private const val KEY_EMAIL_ADDRESS = "email_address"
        private const val KEY_EMAIL_PASSWORD = "email_password"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_RECIPIENT_EMAIL = "recipient_email"
        
        private const val DEFAULT_SMTP_HOST = "smtp.gmail.com"
        private const val DEFAULT_SMTP_PORT = "587"
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    
    /**
     * Get encrypted shared preferences for storing email settings
     */
    private fun getEncryptedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Save email settings
     */
    fun saveEmailSettings(
        emailAddress: String,
        password: String,
        smtpHost: String = DEFAULT_SMTP_HOST,
        smtpPort: String = DEFAULT_SMTP_PORT,
        recipientEmail: String
    ) {
        val prefs = getEncryptedPreferences()
        prefs.edit().apply {
            putString(KEY_EMAIL_ADDRESS, emailAddress)
            putString(KEY_EMAIL_PASSWORD, password)
            putString(KEY_SMTP_HOST, smtpHost)
            putString(KEY_SMTP_PORT, smtpPort)
            putString(KEY_RECIPIENT_EMAIL, recipientEmail)
        }.apply()
    }
    
    /**
     * Get email settings
     */
    private fun getEmailSettings(): EmailSettings? {
        val prefs = getEncryptedPreferences()
        val emailAddress = prefs.getString(KEY_EMAIL_ADDRESS, null)
        val password = prefs.getString(KEY_EMAIL_PASSWORD, null)
        val smtpHost = prefs.getString(KEY_SMTP_HOST, DEFAULT_SMTP_HOST)
        val smtpPort = prefs.getString(KEY_SMTP_PORT, DEFAULT_SMTP_PORT)
        val recipientEmail = prefs.getString(KEY_RECIPIENT_EMAIL, null)
        
        if (emailAddress.isNullOrEmpty() || password.isNullOrEmpty() || recipientEmail.isNullOrEmpty()) {
            return null
        }
        
        return EmailSettings(
            emailAddress = emailAddress,
            password = password,
            smtpHost = smtpHost ?: DEFAULT_SMTP_HOST,
            smtpPort = smtpPort ?: DEFAULT_SMTP_PORT,
            recipientEmail = recipientEmail
        )
    }
    
    /**
     * Generate CSV content from aggregates
     */
    fun generateCsvContent(aggregates: List<DailyAggregate>): String {
        val writer = StringWriter()
        val csvWriter = CSVWriter(writer)
        
        // Write header
        csvWriter.writeNext(arrayOf("screen_name", "start_time", "end_time", "total_uptime_min"))
        
        // Write data rows
        aggregates.forEach { aggregate ->
            csvWriter.writeNext(arrayOf(
                aggregate.screenName,
                dateFormat.format(aggregate.startTime),
                dateFormat.format(aggregate.endTime),
                aggregate.totalUptimeMinutes.toString()
            ))
        }
        
        csvWriter.close()
        return writer.toString()
    }
    
    /**
     * Save CSV to file
     */
    suspend fun saveCsvToFile(csvContent: String): File = withContext(Dispatchers.IO) {
        val fileName = "uptime_report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileWriter(file).use { writer ->
            writer.write(csvContent)
        }
        
        return@withContext file
    }
    
    /**
     * Send email with CSV report
     */
    suspend fun sendEmailReport(aggregates: List<DailyAggregate>): Boolean = withContext(Dispatchers.IO) {
        if (aggregates.isEmpty()) {
            return@withContext false
        }
        
        val settings = getEmailSettings() ?: return@withContext false
        
        try {
            val csvContent = generateCsvContent(aggregates)
            val csvFile = saveCsvToFile(csvContent)
            
            // Set up mail properties
            val properties = Properties()
            properties["mail.smtp.auth"] = "true"
            properties["mail.smtp.starttls.enable"] = "true"
            properties["mail.smtp.host"] = settings.smtpHost
            properties["mail.smtp.port"] = settings.smtpPort
            
            // Create session
            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(settings.emailAddress, settings.password)
                }
            })
            
            // Create message
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(settings.emailAddress))
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(settings.recipientEmail)
            )
            
            // Set subject
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            message.subject = "TV Screen Uptime Report - $today"
            
            // Create multipart message
            val multipart = MimeMultipart()
            
            // Text part
            val textPart = MimeBodyPart()
            textPart.setText("Please find attached the TV screen uptime report.")
            multipart.addBodyPart(textPart)
            
            // Attachment part
            val attachmentPart = MimeBodyPart()
            attachmentPart.attachFile(csvFile)
            multipart.addBodyPart(attachmentPart)
            
            // Set content
            message.setContent(multipart)
            
            // Send message
            Transport.send(message)
            
            // Log successful email
            val emailLog = EmailLog(
                recipient = settings.recipientEmail,
                success = true,
                recordCount = aggregates.size
            )
            emailLogDao.insert(emailLog)
            
            return@withContext true
        } catch (e: Exception) {
            // Log failed email
            val emailLog = EmailLog(
                recipient = settings.recipientEmail,
                success = false,
                errorMessage = e.message ?: "Unknown error",
                recordCount = aggregates.size
            )
            emailLogDao.insert(emailLog)
            
            return@withContext false
        }
    }
    
    /**
     * Get all email logs
     */
    fun getAllEmailLogs() = emailLogDao.getAllEmailLogs()
    
    /**
     * Delete email logs older than a week
     */
    suspend fun deleteEmailLogsOlderThanOneWeek(): Int {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        return emailLogDao.deleteEmailLogsOlderThan(calendar.time)
    }
    
    /**
     * Data class for email settings
     */
    data class EmailSettings(
        val emailAddress: String,
        val password: String,
        val smtpHost: String,
        val smtpPort: String,
        val recipientEmail: String
    )
}
