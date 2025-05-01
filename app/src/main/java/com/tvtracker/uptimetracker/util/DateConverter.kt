package com.tvtracker.uptimetracker.util

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converter for Room database to convert between Date and Long
 */
class DateConverter {
    
    /**
     * Convert from Date to Long timestamp
     */
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * Convert from Long timestamp to Date
     */
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}
