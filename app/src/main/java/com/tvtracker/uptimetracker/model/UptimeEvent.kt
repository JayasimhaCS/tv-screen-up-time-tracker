package com.tvtracker.uptimetracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a single uptime event recorded every 5 minutes
 */
@Entity(tableName = "uptime_events")
data class UptimeEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Name of the screen/device from OS environment
    val screenName: String,
    
    // Timestamp when the event was recorded
    val timestamp: Date,
    
    // Type of event (e.g., "ACTIVE", "IDLE", "STANDBY")
    val eventType: String,
    
    // Flag to mark if this event has been processed in daily aggregation
    val processed: Boolean = false
)
