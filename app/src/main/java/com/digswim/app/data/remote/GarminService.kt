package com.digswim.app.data.remote

import com.digswim.app.data.remote.model.GarminActivityDto
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface GarminService {
    
    // ... (SSO methods unchanged) ...

    // 1. Initial SSO page to get CSRF token
    @GET("https://sso.garmin.cn/sso/signin")
    fun getSsoPage(
        @Query("service") service: String = "https://connect.garmin.cn/modern",
        @Query("webauth-html") webauthHtml: String = "true",
        @Query("webauth-urls") webauthUrls: String = "true",
        @Query("gauth-host") gauthHost: String = "https://sso.garmin.cn/sso",
        @Query("source") source: String = "https://connect.garmin.cn/signin",
        @Query("redirectAfterAccountLoginUrl") redirect: String = "https://connect.garmin.cn/modern"
    ): Call<ResponseBody>

    // 2. Submit credentials
    @FormUrlEncoded
    @POST("https://sso.garmin.cn/sso/signin")
    @Headers("origin: https://sso.garmin.cn")
    fun login(
        @Query("service") service: String = "https://connect.garmin.cn/modern",
        @Query("webauth-html") webauthHtml: String = "true",
        @Query("webauth-urls") webauthUrls: String = "true",
        @Query("gauth-host") gauthHost: String = "https://sso.garmin.cn/sso",
        @Query("source") source: String = "https://connect.garmin.cn/signin",
        @Query("redirectAfterAccountLoginUrl") redirect: String = "https://connect.garmin.cn/modern",
        @FieldMap fields: Map<String, String>
    ): Call<ResponseBody>

    // 3. Ticket Exchange
    @GET
    fun exchangeTicket(@Url url: String): Call<ResponseBody>

    // 3.5 Get Dashboard to extract connect-csrf-token
    @GET("https://connect.garmin.cn/modern/")
    fun getDashboard(): Call<ResponseBody>

    // 4. Get Activities (Updated to return DTO list)
    @GET("https://connect.garmin.cn/modern/proxy/activitylist-service/activities/search/activities")
    fun getActivities(
        @Header("connect-csrf-token") csrfToken: String,
        @Query("activityType") activityType: String = "swimming",
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("limit") limit: Int = 1000,
        @Query("start") start: Int = 0,
        @Query("excludeChildren") excludeChildren: Boolean = false
    ): Call<List<GarminActivityDto>> 

    // Legacy method for testing (can be removed later or kept)
    @GET("https://connect.garmin.cn/modern/proxy/activitylist-service/activities/search/activities")
    fun getRecentActivities(
        @Header("connect-csrf-token") csrfToken: String,
        @Query("limit") limit: Int = 10,
        @Query("start") start: Int = 0
    ): Call<ResponseBody>

    // 5. Get Activity Splits
    @GET("https://connect.garmin.cn/modern/proxy/activity-service/activity/{activityId}/splits")
    fun getActivitySplits(
        @Header("connect-csrf-token") csrfToken: String,
        @Path("activityId") activityId: Long
    ): Call<com.digswim.app.data.remote.model.GarminSplitsResponse>
}
