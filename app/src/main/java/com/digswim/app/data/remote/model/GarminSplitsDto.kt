package com.digswim.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class GarminSplitsResponse(
    @SerializedName("lapDTOs") val laps: List<GarminLapDto>?,
    @SerializedName("lengthDTOs") val lengths: List<GarminLengthDto>?
)

data class GarminLapDto(
    @SerializedName("lapIndex") val lapIndex: Int?,
    @SerializedName("startTimeGMT") val startTimeGMT: String?,
    @SerializedName("distance") val distance: Double?,
    @SerializedName("duration") val duration: Double?,
    @SerializedName("averageSpeed") val averageSpeed: Double?,
    @SerializedName("averageHR") val averageHR: Double?,
    @SerializedName("maxHR") val maxHR: Double?,
    @SerializedName("averageSWOLF") val averageSwolf: Double?,
    @SerializedName("totalNumberOfStrokes") val totalStrokes: Double?,
    @SerializedName("calories") val calories: Double?,
    @SerializedName("lengthDTOs") val lengths: List<GarminLengthDto>?
)

data class GarminLengthDto(
    @SerializedName("lengthIndex") val lengthIndex: Int?,
    @SerializedName("startTimeGMT") val startTimeGMT: String?,
    @SerializedName("duration") val duration: Double?,
    @SerializedName("averageSpeed") val averageSpeed: Double?,
    @SerializedName("averageSWOLF") val averageSwolf: Double?,
    @SerializedName("totalNumberOfStrokes") val strokes: Double?,
    @SerializedName("averageHR") val averageHR: Double?,
    @SerializedName("maxHR") val maxHR: Double?
)
