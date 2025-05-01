package com.tvtracker.uptimetracker.database

import androidx.room.*
import com.tvtracker.uptimetracker.model.DailyAggregate
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for DailyAggregate entity
 */
@Dao
interface DailyAggregateDao {
    
    /**
     * Insert a new daily aggregate
     */
    @Insert
    suspend fun insert(aggregate: DailyAggregate): Long
    
    /**
     * Insert multiple daily aggregates
     */
    @Insert
    suspend fun insertAll(aggregates: List<DailyAggregate>): List<Long>
    
    /**
     * Update an existing daily aggregate
     */
    @Update
    suspend fun update(aggregate: DailyAggregate)
    
    /**
     * Delete a daily aggregate
     */
    @Delete
    suspend fun delete(aggregate: DailyAggregate)
    
    /**
     * Get all daily aggregates
     */
    @Query("SELECT * FROM daily_aggregates ORDER BY startTime DESC")
    fun getAllAggregates(): Flow<List<DailyAggregate>>
    
    /**
     * Get daily aggregates for a specific day
     */
    @Query("SELECT * FROM daily_aggregates WHERE date(startTime/1000, 'unixepoch') = date(:date/1000, 'unixepoch') ORDER BY startTime ASC")
    suspend fun getAggregatesForDay(date: Date): List<DailyAggregate>
    
    /**
     * Get unsent daily aggregates (for email reporting)
     */
    @Query("SELECT * FROM daily_aggregates WHERE reportSent = 0 ORDER BY startTime ASC")
    suspend fun getUnsentAggregates(): List<DailyAggregate>
    
    /**
     * Mark aggregates as sent
     */
    @Query("UPDATE daily_aggregates SET reportSent = 1 WHERE id IN (:aggregateIds)")
    suspend fun markAggregatesAsSent(aggregateIds: List<Long>)
    
    /**
     * Delete aggregates older than a specific date
     */
    @Query("DELETE FROM daily_aggregates WHERE createdDate < :date")
    suspend fun deleteAggregatesOlderThan(date: Date): Int
    
    /**
     * Get aggregates for CSV report
     */
    @Query("SELECT * FROM daily_aggregates WHERE reportSent = 0 AND startTime BETWEEN :startDate AND :endDate ORDER BY screenName, startTime ASC")
    suspend fun getAggregatesForReport(startDate: Date, endDate: Date): List<DailyAggregate>
}
