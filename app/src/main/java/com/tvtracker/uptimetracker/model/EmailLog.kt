package com.tvtracker.uptimetracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity to track email sending attempts and status
 */
@Entity(tableName = "email_logs")
data class EmailLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // When the email was sent
    val sentTime: Date = Date(),
    
    // Email recipient
    val recipient: String,
    
    // Whether the email was sent successfully
    val success: Boolean,
    
    // Error message if sending failed
    val errorMessage: String? = null,
    
    // Number of records included in the email
    val recordCount: Int = 0
)
