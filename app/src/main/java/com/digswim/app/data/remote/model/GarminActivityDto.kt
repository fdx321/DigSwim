package com.digswim.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class GarminActivityDto(
    @SerializedName("activityId") val activityId: Long,
    @SerializedName("activityName") val activityName: String?,
    @SerializedName("startTimeLocal") val startTimeLocal: String, // "2025-12-30 10:31:49"
    @SerializedName("duration") val duration: Double?, // Seconds
    @SerializedName("distance") val distance: Double?, // Meters
    @SerializedName("calories") val calories: Double?,
    @SerializedName("averageHR") val averageHR: Double?,
    @SerializedName("maxHR") val maxHR: Double?,
    @SerializedName("averageSpeed") val averageSpeed: Double?, // m/s
    @SerializedName("averageSwolf") val averageSwolf: Double?,
    @SerializedName("strokes") val strokes: Double?,
    @SerializedName("poolLength") val poolLength: Double?,
    @SerializedName("unitOfPoolLength") val unitOfPoolLength: UnitDto?,
    @SerializedName("activityType") val activityType: ActivityTypeDto?
)

data class UnitDto(
    @SerializedName("unitKey") val unitKey: String?,
    @SerializedName("factor") val factor: Double?
)

data class ActivityTypeDto(
    @SerializedName("typeKey") val typeKey: String?,
    @SerializedName("typeId") val typeId: Int?
)
