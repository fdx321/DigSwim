package com.digswim.app.model

import java.time.LocalDateTime

enum class SwimType {
    POOL, OPEN_WATER
}

data class SwimActivity(
    val id: String,
    val type: SwimType,
    val activityName: String? = null,
    val startTime: LocalDateTime,
    val distanceMeters: Int,
    val durationSeconds: Long,
    val calories: Int,
    val avgPaceSecondsPer100m: Int, // e.g. 120 = 2:00/100m
    val avgHeartRate: Int? = null, // Added field
    val swolf: Int? = null,
    val totalStrokes: Int? = null
)

data class WeeklySummary(
    val weekStart: LocalDateTime,
    val weekEnd: LocalDateTime,
    val totalDistanceMeters: Int,
    val totalDurationSeconds: Long,
    val swimCount: Int,
    val totalCalories: Int,
    val avgPaceSecondsPer100m: Int,
    val avgHeartRate: Int,
    val avgSwolf: Int,
    val dailyDistances: List<Int> // 7 days, 0 if no swim
)

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalDistanceMeters: Int,
    val totalDurationSeconds: Long,
    val swimCount: Int,
    val totalCalories: Int,
    val avgPaceSecondsPer100m: Int,
    val avgHeartRate: Int,
    val avgSwolf: Int,
    val dailyDistances: List<Int> // 28-31 days, 0 if no swim
)

data class YearlySummary(
    val year: Int,
    val totalDistanceMeters: Int,
    val totalDurationSeconds: Long,
    val swimCount: Int,
    val totalCalories: Int,
    val avgPaceSecondsPer100m: Int,
    val avgHeartRate: Int,
    val avgSwolf: Int,
    val monthlyDistances: List<Int> // 12 months, 0 if no swim
)

// Detail Models
data class SwimActivityDetail(
    val summary: SwimActivity,
    val laps: List<SwimLap>,
    val metrics: SwimMetrics
)

data class SwimLap(
    val index: Int,
    val durationSeconds: Double,
    val distanceMeters: Double,
    val paceSecondsPer100m: Int,
    val avgHeartRate: Int?,
    val strokeCount: Int?,
    val swolf: Int?
)

data class SwimMetrics(
    // Using cumulative distance as X-axis usually for swim charts
    val pacePoints: List<MetricPoint>, 
    val hrPoints: List<MetricPoint>,
    val swolfPoints: List<MetricPoint>
)

data class MetricPoint(
    val xValue: Double, // Cumulative Distance (m) or Time (s)
    val yValue: Double
)
