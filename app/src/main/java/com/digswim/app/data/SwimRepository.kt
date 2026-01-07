package com.digswim.app.data

import com.digswim.app.model.MonthlySummary
import com.digswim.app.model.SwimActivity
import com.digswim.app.model.WeeklySummary
import com.digswim.app.model.YearlySummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

import com.digswim.app.model.SwimActivityDetail

interface SwimRepository {
    fun getWeeklySummaries(): Flow<List<WeeklySummary>>
    
    // New methods for specific week
    fun getActivitiesForWeek(weekStart: LocalDate): Flow<List<SwimActivity>>
    fun getWeeklySummary(weekStart: LocalDate): Flow<WeeklySummary>
    
    // Methods for specific month
    fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary>

    // Methods for specific year
    fun getYearlySummary(year: Int): Flow<YearlySummary>
    
    // Get all activities sorted by time descending
    fun getAllActivities(): Flow<List<SwimActivity>>
    
    // Get details for a specific activity
    suspend fun getActivityDetail(activityId: String): SwimActivityDetail?
    
    // Refresh data from remote source
    suspend fun refreshData()

    // Load older data (pagination)
    suspend fun loadMore()
}
