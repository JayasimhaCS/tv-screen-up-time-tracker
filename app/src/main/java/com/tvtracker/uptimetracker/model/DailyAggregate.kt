package com.tvtracker.uptimetracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing aggregated uptime data for a continuous period
 * This matches the required CSV output format:
 * screen_name,start_time,end_time,total_uptime_min
 */
@Entity(tableName = "daily_aggregates")
data class DailyAggregate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Name of the screen/device from OS environment
    val screenName: String,
    
    // Start time of the continuous uptime period
    val startTime: Date,
    
    // End time of the continuous uptime period
    val endTime: Date,
    
    // Total uptime in minutes for this period
    val totalUptimeMinutes: Int,
    
    // Date when this aggregate was created (for weekly cleanup)
    val createdDate: Date = Date(),
    
    // Flag to track if this data has been sent via email
    val reportSent: Boolean = false
)
