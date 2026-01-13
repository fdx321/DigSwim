package com.digswim.app.data.remote

import android.content.Context
import android.util.Log
import com.digswim.app.data.SwimRepository
import com.digswim.app.data.local.UserPreferences
import com.digswim.app.data.remote.model.GarminActivityDto
import com.digswim.app.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarminSwimRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val garminService: GarminService,
    private val sessionManager: GarminSessionManager,
    private val userPreferences: UserPreferences
) : SwimRepository {

    companion object {
        private const val TAG = "GarminSwimRepo"
        private const val MIN_HISTORY_YEAR = 2015
        private const val ACTIVITY_TYPE_SWIMMING = "swimming"
        private const val ACTIVITY_TYPE_LAP_SWIMMING = "lap_swimming"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        private const val CACHE_FILE_NAME = "swim_activities_cache.json"
    }

    // Simple in-memory cache
    private val cachedActivities = MutableStateFlow<List<SwimActivity>>(emptyList())
    private val loadedYears = mutableSetOf<Int>()
    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)

    private var isCacheLoaded = false
    private val cacheMutex = Mutex()

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, object : TypeAdapter<LocalDateTime>() {
            override fun write(out: JsonWriter, value: LocalDateTime?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    out.value(value.format(dateFormatter))
                }
            }

            override fun read(input: JsonReader): LocalDateTime? {
                if (input.peek() == com.google.gson.stream.JsonToken.NULL) {
                    input.nextNull()
                    return null
                }
                return LocalDateTime.parse(input.nextString(), dateFormatter)
            }
        })
        .create()

    private suspend fun ensureCacheLoaded() {
        if (isCacheLoaded) return
        cacheMutex.withLock {
            if (isCacheLoaded) return
            Log.d(TAG, "Attempting to load cache from disk...")
            loadCacheFromDisk()
            isCacheLoaded = true
        }
    }

    private suspend fun loadCacheFromDisk() = withContext(Dispatchers.IO) {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        if (cacheFile.exists()) {
            try {
                FileReader(cacheFile).use { reader ->
                    val type = object : TypeToken<List<SwimActivity>>() {}.type
                    val activities: List<SwimActivity>? = gson.fromJson(reader, type)
                    if (activities != null) {
                        cachedActivities.value = activities
                        // Re-populate loadedYears based on loaded data
                        val years = activities.map { it.startTime.year }.toSet()
                        loadedYears.addAll(years)
                        Log.d(TAG, "CACHE HIT: Loaded ${activities.size} activities from disk cache. Covered years: $years")
                    } else {
                        Log.w(TAG, "CACHE MISS: Cache file exists but parsed content is null")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "CACHE ERROR: Error loading cache from disk", e)
            }
        } else {
             Log.d(TAG, "CACHE MISS: No cache file found at ${cacheFile.absolutePath}")
        }
    }

    private suspend fun saveCacheToDisk(activities: List<SwimActivity>) = withContext(Dispatchers.IO) {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        try {
            FileWriter(cacheFile).use { writer ->
                gson.toJson(activities, writer)
            }
            Log.d(TAG, "CACHE WRITE: Saved ${activities.size} activities to disk cache at ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "CACHE ERROR: Error saving cache to disk", e)
        }
    }

    private suspend fun ensureDataLoaded(year: Int = LocalDate.now().year, forceRefresh: Boolean = false) = withContext(Dispatchers.IO) {
        ensureCacheLoaded()

        if (!sessionManager.hasSession()) {
            val success = autoLogin()
            if (!success) {
                Log.w(TAG, "Auto-login failed or no credentials. Please set in Profile.")
                return@withContext
            }
        }
        
        // Avoid re-fetching if we have data for this year
        if (!forceRefresh && loadedYears.contains(year)) {
             Log.d(TAG, "CACHE HIT: Data for year $year already loaded in memory (or from disk). Skipping fetch.")
             return@withContext
        }

        Log.d(TAG, "NETWORK FETCH: Starting fetch for year $year (forceRefresh=$forceRefresh)...")
        try {
            val token = sessionManager.getCsrfToken()

            
            val response = garminService.getActivities(
                csrfToken = token,
                activityType = ACTIVITY_TYPE_SWIMMING,
                startDate = "$year-01-01",
                endDate = "$year-12-31",
                limit = 100, 
                start = 0
            ).execute()
            
            if (response.isSuccessful) {
                val activitiesDto = response.body() ?: emptyList()
                
                // Filter for swimming and map to domain model
                val swimActivities = activitiesDto
                    .filter { it.activityType?.typeKey == ACTIVITY_TYPE_SWIMMING || it.activityType?.typeKey == ACTIVITY_TYPE_LAP_SWIMMING }
                    .mapNotNull { dto -> mapDtoToModel(dto) }
                
                // Merge with existing
                val current = cachedActivities.value
                val merged = (current + swimActivities)
                    .distinctBy { it.id }
                    .sortedByDescending { it.startTime }
                
                cachedActivities.value = merged
                loadedYears.add(year)
                
                // Save updated cache to disk
                saveCacheToDisk(merged)
            } else {
                Log.e(TAG, "Failed to fetch activities: ${response.code()}")
                Log.e(TAG, "Error body: ${response.errorBody()?.string()}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching data", e)
        }
    }

    override suspend fun loadMore() {
        val minLoaded = loadedYears.minOrNull() ?: LocalDate.now().year
        val nextYear = minLoaded - 1
        
        if (nextYear < MIN_HISTORY_YEAR) {
            return
        }
        ensureDataLoaded(nextYear)
    }

    private suspend fun autoLogin(): Boolean {
        val email = userPreferences.garminEmail.first()
        val password = userPreferences.garminPassword.first()
        
        if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
            Log.w(TAG, "No saved credentials found.")
            return false
        }
        
        return try {
            // 1. Get SSO Page
            val ssoResponse = garminService.getSsoPage().execute()
            if (!ssoResponse.isSuccessful) return false
            val ssoBody = ssoResponse.body()?.string() ?: ""
            val csrfToken = extractCsrfToken(ssoBody) 
            if (csrfToken == null) return false
            
            // 2. Submit Credentials
            val loginResponse = garminService.login(
                fields = mapOf(
                    "username" to email,
                    "password" to password,
                    "embed" to "true",
                    "_csrf" to csrfToken
                )
            ).execute()
            
            if (!loginResponse.isSuccessful) {
                 return false
            }
            
            // 3. Ticket Exchange
            var ticket = extractTicketFromResponse(loginResponse)
            
            // If ticket not found in URL (redirect), try to parse it from body content
            if (ticket == null) {
                val loginBody = loginResponse.body()?.string() ?: ""

                // Try to find 'response_url' variable first as it's the most reliable source in the JS block
                var pattern = java.util.regex.Pattern.compile("var\\s+response_url\\s*=\\s*\"[^\"]*ticket=(ST-[^\"]+)\"")
                var matcher = pattern.matcher(loginBody)
                
                if (matcher.find()) {
                    ticket = matcher.group(1)
                } else {
                    // Fallback to searching for any ticket=ST-... pattern
                    pattern = java.util.regex.Pattern.compile("ticket=(ST-[^\"'&\\s;]+)")
                    matcher = pattern.matcher(loginBody)
                    if (matcher.find()) {
                        ticket = matcher.group(1)
                    }
                }
            }

            if (ticket != null) {
                val exchangeUrl = "https://connect.garmin.cn/modern?ticket=$ticket"
                garminService.exchangeTicket(exchangeUrl).execute()
            }
            
            // 4. Get Dashboard for connect-csrf-token
            val dashboardResponse = garminService.getDashboard().execute()

            if (dashboardResponse.isSuccessful) {
                val html = dashboardResponse.body()?.string() ?: ""
                
                var token: String? = null
                val matcher = java.util.regex.Pattern.compile("CSRF_TOKEN\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(html)
                if (matcher.find()) {
                    token = matcher.group(1)
                }
                
                // Fallback: use the robust HTML tag extraction (same as SSO) if JS extraction fails
                if (token.isNullOrEmpty()) {
                    token = extractCsrfToken(html)
                }

                if (!token.isNullOrEmpty()) {
                    sessionManager.setCsrfToken(token)
                    Log.d(TAG, "Auto-login successful.")
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Auto-login exception", e)
            false
        }
    }

    private fun extractCsrfToken(html: String): String? {
        // Try strict pattern first: name="_csrf" ... value="..."
        var pattern = java.util.regex.Pattern.compile("name=[\"']_csrf[\"']\\s+value=[\"']([^\"']+)[\"']")
        var matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        
        // Try reverse pattern: value="..." ... name="_csrf"
        pattern = java.util.regex.Pattern.compile("value=[\"']([^\"']+)[\"']\\s+name=[\"']_csrf[\"']")
        matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        
        // Try finding input tag with name="_csrf" and extracting value
        // Regex: <input [^>]*name=["']_csrf["'][^>]*value=["']([^"']+)["']
        pattern = java.util.regex.Pattern.compile("<input[^>]*name=[\"']_csrf[\"'][^>]*value=[\"']([^\"']+)[\"']")
        matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        
        // Try meta tag: <meta name="csrf-token" content="...">
        pattern = java.util.regex.Pattern.compile("<meta[^>]*name=[\"']csrf-token[\"'][^>]*content=[\"']([^\"']+)[\"']")
        matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        
        return null
    }

    private fun extractTicketFromResponse(response: retrofit2.Response<okhttp3.ResponseBody>): String? {
        val finalUrl = response.raw().request.url.toString()
        val ticketMatcher = java.util.regex.Pattern.compile("ticket=([^&]+)").matcher(finalUrl)
        if (ticketMatcher.find()) return ticketMatcher.group(1)
        return null
    }

    private fun mapDtoToModel(dto: GarminActivityDto): SwimActivity? {
        return try {
            val startTime = LocalDateTime.parse(dto.startTimeLocal, dateFormatter)
            
            // Calculate Pace (seconds per 100m)
            // averageSpeed is m/s. 
            // Pace = 100m / speed(m/s) = seconds
            val pace = if (dto.averageSpeed != null && dto.averageSpeed > 0) {
                (100.0 / dto.averageSpeed).toInt()
            } else {
                0
            }

            SwimActivity(
                id = dto.activityId.toString(),
                type = SwimType.POOL, // Default to POOL as API filter is 'swimming' (usually lap swimming)
                activityName = dto.activityName,
                startTime = startTime,
                durationSeconds = dto.duration?.toLong() ?: 0L,
                distanceMeters = dto.distance?.toInt() ?: 0,
                calories = dto.calories?.toInt() ?: 0,
                avgHeartRate = dto.averageHR?.toInt(),
                avgPaceSecondsPer100m = pace,
                totalStrokes = dto.strokes?.toInt(),
                swolf = dto.averageSwolf?.toInt()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping activity ${dto.activityId}", e)
            null
        }
    }

    override fun getWeeklySummaries(): Flow<List<WeeklySummary>> = flow {
        // Not implemented or needed for the new UI flow, but required by interface
        // We can implement it if needed by grouping all activities by week
        ensureDataLoaded()
        emit(emptyList()) // Placeholder
    }

    override fun getActivitiesForWeek(weekStart: LocalDate): Flow<List<SwimActivity>> = cachedActivities
        .onStart { ensureDataLoaded(weekStart.year) }
        .map { activities ->
            val weekEnd = weekStart.plusDays(6)
            activities.filter {
                val date = it.startTime.toLocalDate()
                !date.isBefore(weekStart) && !date.isAfter(weekEnd)
            }.sortedByDescending { it.startTime }
        }

    override fun getWeeklySummary(weekStart: LocalDate): Flow<WeeklySummary> = cachedActivities
        .onStart { ensureDataLoaded(weekStart.year) }
        .map { activities ->
            val endOfWeek = weekStart.plusDays(6)
            
            val weeklyActivities = activities.filter { 
                val date = it.startTime.toLocalDate()
                !date.isBefore(weekStart) && !date.isAfter(endOfWeek)
            }

            if (weeklyActivities.isEmpty()) {
                // Return empty summary instead of null to match non-nullable return type
                WeeklySummary(
                    weekStart = weekStart.atStartOfDay(),
                    weekEnd = endOfWeek.atTime(23, 59, 59),
                    totalDistanceMeters = 0,
                    totalDurationSeconds = 0L,
                    swimCount = 0,
                    totalCalories = 0,
                    avgPaceSecondsPer100m = 0,
                    avgHeartRate = 0,
                    avgSwolf = 0,
                    dailyDistances = List(7) { 0 }
                )
            } else {
                // Re-calculate summary from activities
                val totalDist = weeklyActivities.sumOf { it.distanceMeters }
                val totalDur = weeklyActivities.sumOf { it.durationSeconds }
                val totalCals = weeklyActivities.sumOf { it.calories }
                val swimCount = weeklyActivities.size
                
                // Avg Pace
                val validPaceActivities = weeklyActivities.filter { it.avgPaceSecondsPer100m > 0 }
                val avgPace = if (validPaceActivities.isNotEmpty()) {
                    validPaceActivities.sumOf { it.avgPaceSecondsPer100m } / validPaceActivities.size
                } else 0
                
                // Avg HR
                val validHrActivities = weeklyActivities.filter { it.avgHeartRate != null && it.avgHeartRate > 0 }
                val avgHr = if (validHrActivities.isNotEmpty()) {
                    validHrActivities.sumOf { it.avgHeartRate!! } / validHrActivities.size
                } else 0
                
                // Avg Swolf
                val validSwolfActivities = weeklyActivities.filter { it.swolf != null && it.swolf > 0 }
                val avgSwolf = if (validSwolfActivities.isNotEmpty()) {
                    validSwolfActivities.sumOf { it.swolf!! } / validSwolfActivities.size
                } else 0
                
                // Calculate daily distances
                val dailyDistances = IntArray(7) { 0 }
                weeklyActivities.forEach { act ->
                    val dayIndex = act.startTime.dayOfWeek.value - 1 // Mon=0
                    if (dayIndex in 0..6) {
                        dailyDistances[dayIndex] += act.distanceMeters
                    }
                }
                
                WeeklySummary(
                    weekStart = weekStart.atStartOfDay(),
                    weekEnd = endOfWeek.atTime(23, 59, 59),
                    totalDistanceMeters = totalDist,
                    totalDurationSeconds = totalDur,
                    swimCount = swimCount,
                    totalCalories = totalCals,
                    avgPaceSecondsPer100m = avgPace,
                    avgHeartRate = avgHr,
                    avgSwolf = avgSwolf,
                    dailyDistances = dailyDistances.toList()
                )
            }
        }

    override fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary> = cachedActivities
        .onStart { ensureDataLoaded(year) }
        .map { activities ->
            val yearMonth = YearMonth.of(year, month)
            
            val monthlyActivities = activities.filter { 
                val date = it.startTime.toLocalDate()
                YearMonth.from(date) == yearMonth
            }
            
            if (monthlyActivities.isEmpty()) {
                MonthlySummary(
                    year = year,
                    month = month,
                    totalDistanceMeters = 0,
                    totalDurationSeconds = 0L,
                    swimCount = 0,
                    totalCalories = 0,
                    avgPaceSecondsPer100m = 0,
                    avgHeartRate = 0,
                    avgSwolf = 0,
                    dailyDistances = List(yearMonth.lengthOfMonth()) { 0 }
                )
            } else {
                val totalDist = monthlyActivities.sumOf { it.distanceMeters }
                val totalDur = monthlyActivities.sumOf { it.durationSeconds }
                val totalCals = monthlyActivities.sumOf { it.calories }
                val swimCount = monthlyActivities.size
                
                // Avg Pace
                val validPaceActivities = monthlyActivities.filter { it.avgPaceSecondsPer100m > 0 }
                val avgPace = if (validPaceActivities.isNotEmpty()) {
                    validPaceActivities.sumOf { it.avgPaceSecondsPer100m } / validPaceActivities.size
                } else 0
                
                // Avg HR
                val validHrActivities = monthlyActivities.filter { it.avgHeartRate != null && it.avgHeartRate > 0 }
                val avgHr = if (validHrActivities.isNotEmpty()) {
                    validHrActivities.sumOf { it.avgHeartRate!! } / validHrActivities.size
                } else 0
                
                // Avg Swolf
                val validSwolfActivities = monthlyActivities.filter { it.swolf != null && it.swolf > 0 }
                val avgSwolf = if (validSwolfActivities.isNotEmpty()) {
                    validSwolfActivities.sumOf { it.swolf!! } / validSwolfActivities.size
                } else 0

                val daysInMonth = yearMonth.lengthOfMonth()
                val dailyDistances = IntArray(daysInMonth) { 0 }
                monthlyActivities.forEach { act ->
                    val dayIndex = act.startTime.dayOfMonth - 1
                    if (dayIndex in 0 until daysInMonth) {
                        dailyDistances[dayIndex] += act.distanceMeters
                    }
                }

                MonthlySummary(
                    year = year,
                    month = month,
                    totalDistanceMeters = totalDist,
                    totalDurationSeconds = totalDur,
                    swimCount = swimCount,
                    totalCalories = totalCals,
                    avgPaceSecondsPer100m = avgPace,
                    avgHeartRate = avgHr,
                    avgSwolf = avgSwolf,
                    dailyDistances = dailyDistances.toList()
                )
            }
        }

    override fun getYearlySummary(year: Int): Flow<YearlySummary> = cachedActivities
        .onStart { ensureDataLoaded(year) }
        .map { activities ->
            val yearlyActivities = activities.filter { 
                it.startTime.year == year
            }
            
            if (yearlyActivities.isEmpty()) {
                YearlySummary(
                    year = year,
                    totalDistanceMeters = 0,
                    totalDurationSeconds = 0L,
                    swimCount = 0,
                    totalCalories = 0,
                    avgPaceSecondsPer100m = 0,
                    avgHeartRate = 0,
                    avgSwolf = 0,
                    monthlyDistances = List(12) { 0 }
                )
            } else {
                 val totalDist = yearlyActivities.sumOf { it.distanceMeters }
                val totalDur = yearlyActivities.sumOf { it.durationSeconds }
                val totalCals = yearlyActivities.sumOf { it.calories }
                val swimCount = yearlyActivities.size
                
                // Avg Pace
                val validPaceActivities = yearlyActivities.filter { it.avgPaceSecondsPer100m > 0 }
                val avgPace = if (validPaceActivities.isNotEmpty()) {
                    validPaceActivities.sumOf { it.avgPaceSecondsPer100m } / validPaceActivities.size
                } else 0
                
                // Avg HR
                val validHrActivities = yearlyActivities.filter { it.avgHeartRate != null && it.avgHeartRate > 0 }
                val avgHr = if (validHrActivities.isNotEmpty()) {
                    validHrActivities.sumOf { it.avgHeartRate!! } / validHrActivities.size
                } else 0
                
                // Avg Swolf
                val validSwolfActivities = yearlyActivities.filter { it.swolf != null && it.swolf > 0 }
                val avgSwolf = if (validSwolfActivities.isNotEmpty()) {
                    validSwolfActivities.sumOf { it.swolf!! } / validSwolfActivities.size
                } else 0
                
                val monthlyDistances = IntArray(12) { 0 }
                yearlyActivities.forEach { act ->
                    val monthIndex = act.startTime.monthValue - 1
                    monthlyDistances[monthIndex] += act.distanceMeters
                }
                
                YearlySummary(
                    year = year,
                    totalDistanceMeters = totalDist,
                    totalDurationSeconds = totalDur,
                    swimCount = swimCount,
                    totalCalories = totalCals,
                    avgPaceSecondsPer100m = avgPace,
                    avgHeartRate = avgHr,
                    avgSwolf = avgSwolf,
                    monthlyDistances = monthlyDistances.toList()
                )
            }
        }

    override fun getAllActivities(): Flow<List<SwimActivity>> = cachedActivities
        .onStart { ensureDataLoaded() }

    override suspend fun getActivityDetail(activityId: String): SwimActivityDetail? = withContext(Dispatchers.IO) {
        // 1. Find summary in cache first
        val summary = cachedActivities.value.find { it.id == activityId } ?: return@withContext null
        
        // 2. Fetch splits
        if (!sessionManager.hasSession()) {
            if (!autoLogin()) return@withContext null
        }
        
        try {
            val token = sessionManager.getCsrfToken()
            val response = garminService.getActivitySplits(token, activityId.toLong()).execute()
            
            if (response.isSuccessful) {
                val splitsResponse = response.body()
                
                // Parse Laps
                val laps = splitsResponse?.laps?.mapIndexed { index, dto ->
                    SwimLap(
                        index = dto.lapIndex ?: (index + 1),
                        durationSeconds = dto.duration ?: 0.0,
                        distanceMeters = dto.distance ?: 0.0,
                        paceSecondsPer100m = calculatePace(dto.averageSpeed),
                        avgHeartRate = dto.averageHR?.toInt(),
                        strokeCount = dto.totalStrokes?.toInt(),
                        swolf = dto.averageSwolf?.toInt()
                    )
                } ?: emptyList()
                
                // Parse Metrics for Charts
                val lapsDto = splitsResponse?.laps ?: emptyList()
                
                // Collect all lengths from all laps (flattened)
                val allLengths = lapsDto.flatMap { it.lengths ?: emptyList() }

                val pacePoints = mutableListOf<MetricPoint>()
                val hrPoints = mutableListOf<MetricPoint>()
                val swolfPoints = mutableListOf<MetricPoint>()
                
                if (allLengths.isNotEmpty()) {
                    // Use Lengths for high resolution data
                    var cumDuration = 0.0
                    
                    allLengths.forEach { length ->
                        val dur = length.duration ?: 0.0
                        cumDuration += dur
                        
                        // Pace
                        if (length.averageSpeed != null && length.averageSpeed > 0) {
                             pacePoints.add(MetricPoint(cumDuration, 100.0 / length.averageSpeed))
                        }
                        
                        // HR
                        if (length.averageHR != null && length.averageHR > 0) {
                            hrPoints.add(MetricPoint(cumDuration, length.averageHR))
                        }
                        
                        // Swolf
                        if (length.averageSwolf != null && length.averageSwolf > 0) {
                             swolfPoints.add(MetricPoint(cumDuration, length.averageSwolf))
                        }
                    }
                } else {
                    // Fallback to Laps if no lengths available
                    var cumDuration = 0.0
                    lapsDto.forEach { lap ->
                        val dur = lap.duration ?: 0.0
                        cumDuration += dur
                        
                        if (lap.averageSpeed != null && lap.averageSpeed > 0) {
                             pacePoints.add(MetricPoint(cumDuration, 100.0 / lap.averageSpeed))
                        }
                        
                        // HR
                        if (lap.averageHR != null && lap.averageHR > 0) {
                            hrPoints.add(MetricPoint(cumDuration, lap.averageHR))
                        }
                        
                        // Swolf
                        if (lap.averageSwolf != null && lap.averageSwolf > 0) {
                             swolfPoints.add(MetricPoint(cumDuration, lap.averageSwolf))
                        }
                    }
                }
                
                SwimActivityDetail(
                    summary = summary,
                    laps = laps,
                    metrics = SwimMetrics(pacePoints, hrPoints, swolfPoints)
                )
            } else {
                Log.e(TAG, "Failed to get splits: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting activity details", e)
            null
        }
    }

    private data class MetricSourceItem(
        val duration: Double,
        val distance: Double,
        val calculatedDistance: Double,
        val speed: Double?,
        val hr: Double?,
        val swolf: Double?
    )

    private fun calculatePace(speed: Double?): Int {
        if (speed == null || speed <= 0) return 0
        return (100.0 / speed).toInt()
    }

    override suspend fun refreshData() {
        loadedYears.clear()
        cachedActivities.value = emptyList()
        ensureDataLoaded(LocalDate.now().year, forceRefresh = true)
    }
}
