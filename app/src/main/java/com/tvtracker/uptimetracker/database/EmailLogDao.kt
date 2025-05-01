package com.tvtracker.uptimetracker.database

import androidx.room.*
import com.tvtracker.uptimetracker.model.EmailLog
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for EmailLog entity
 */
@Dao
interface EmailLogDao {
    
    /**
     * Insert a new email log
     */
    @Insert
    suspend fun insert(emailLog: EmailLog): Long
    
    /**
     * Update an existing email log
     */
    @Update
    suspend fun update(emailLog: EmailLog)
    
    /**
     * Delete an email log
     */
    @Delete
    suspend fun delete(emailLog: EmailLog)
    
    /**
     * Get all email logs
     */
    @Query("SELECT * FROM email_logs ORDER BY sentTime DESC")
    fun getAllEmailLogs(): Flow<List<EmailLog>>
    
    /**
     * Get successful email logs
     */
    @Query("SELECT * FROM email_logs WHERE success = 1 ORDER BY sentTime DESC")
    suspend fun getSuccessfulEmails(): List<EmailLog>
    
    /**
     * Get failed email logs
     */
    @Query("SELECT * FROM email_logs WHERE success = 0 ORDER BY sentTime DESC")
    suspend fun getFailedEmails(): List<EmailLog>
    
    /**
     * Delete email logs older than a specific date
     */
    @Query("DELETE FROM email_logs WHERE sentTime < :date")
    suspend fun deleteEmailLogsOlderThan(date: Date): Int
    
    /**
     * Get the most recent successful email log
     */
    @Query("SELECT * FROM email_logs WHERE success = 1 ORDER BY sentTime DESC LIMIT 1")
    suspend fun getMostRecentSuccessfulEmail(): EmailLog?
}
