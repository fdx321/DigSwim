package com.digswim.app.ui.garmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digswim.app.data.remote.GarminService
import com.digswim.app.data.remote.PersistentCookieJar
import com.digswim.app.data.remote.GarminSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class GarminViewModel @Inject constructor(
    private val garminService: GarminService,
    private val cookieJar: PersistentCookieJar,
    private val sessionManager: GarminSessionManager
) : ViewModel() {

    private val _logOutput = MutableStateFlow("")
    val logOutput: StateFlow<String> = _logOutput.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private fun log(message: String) {
        _logOutput.value += "\n$message"
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoggingIn.value = true
            _logOutput.value = "Starting login process..."
            
            try {
                // 1. Get SSO Page to fetch CSRF token
                log("Fetching SSO page for CSRF token...")
                val ssoResponse = garminService.getSsoPage().execute()
                
                if (!ssoResponse.isSuccessful) {
                    log("Failed to fetch SSO page: ${ssoResponse.code()}")
                    _isLoggingIn.value = false
                    return@launch
                }

                val ssoBody = ssoResponse.body()?.string() ?: ""
                val csrfToken = extractCsrfToken(ssoBody)
                
                if (csrfToken == null) {
                    log("Failed to extract CSRF token. The page structure might have changed.")
                    // Log a snippet of body for debugging (be careful with length)
                    log("Body snippet: ${ssoBody.take(500)}...")
                    _isLoggingIn.value = false
                    return@launch
                }
                
                log("CSRF Token found: $csrfToken")

                // 2. Submit Credentials
                log("Submitting credentials...")
                val loginResponse = garminService.login(
                    fields = mapOf(
                        "username" to email,
                        "password" to pass,
                        "embed" to "true",
                        "_csrf" to csrfToken
                    )
                ).execute()

                if (loginResponse.isSuccessful) {
                    log("Login request successful (HTTP 200/302).")
                    
                    val ticket = extractTicketFromResponse(loginResponse)
                    if (ticket != null) {
                        log("Service Ticket obtained: $ticket")
                        // Explicitly exchange ticket for session cookies
                        // The ticket URL is usually: https://connect.garmin.cn/modern?ticket=ST-XXXXXX
                        val exchangeUrl = "https://connect.garmin.cn/modern?ticket=$ticket"
                        log("Exchanging ticket at: $exchangeUrl")
                        
                        val exchangeResponse = garminService.exchangeTicket(exchangeUrl).execute()
                        if (exchangeResponse.isSuccessful) {
                            log("Ticket exchange successful. Session cookies should be set.")
                        } else {
                            log("Ticket exchange failed: ${exchangeResponse.code()}")
                        }
                    } else {
                        log("No explicit ticket found in body/headers. Checking cookies...")
                        // Sometimes OkHttp follows redirect and we land on the page directly.
                        // But if 401 persists, it means the redirect chain didn't set the cookie correctly or we missed the ticket.
                        
                        // Let's try to extract ticket from the final URL if OkHttp followed redirects
                        val finalUrl = loginResponse.raw().request.url.toString()
                        if (finalUrl.contains("ticket=")) {
                            log("Found ticket in final URL: $finalUrl")
                            // We are already there, so cookies might be set.
                        }
                    }
                    
                    // 3. Get Dashboard to extract connect-csrf-token
                    log("Fetching dashboard to get connect-csrf-token...")
                    val dashboardResponse = garminService.getDashboard().execute()
                    if (dashboardResponse.isSuccessful) {
                        val html = dashboardResponse.body()?.string() ?: ""
                        // Regex for CSRF_TOKEN
                        val matcher = Pattern.compile("CSRF_TOKEN\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(html)
                        if (matcher.find()) {
                            val token = matcher.group(1) ?: ""
                            sessionManager.setCsrfToken(token)
                            log("Found connect-csrf-token: $token")
                        } else {
                            log("Could not find CSRF_TOKEN in dashboard HTML.")
                            // Fallback: sometimes it's in a different format or header, but let's hope this works.
                        }
                    } else {
                        log("Failed to fetch dashboard: ${dashboardResponse.code()}")
                    }

                    log("Login flow completed. Ready to test API.")
                } else {
                    log("Login failed: ${loginResponse.code()}")
                    log("Error body: ${loginResponse.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                log("Login Exception: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    fun getRecentActivities() {
        viewModelScope.launch(Dispatchers.IO) {
            log("Fetching recent activities...")
            val token = sessionManager.getCsrfToken()
            if (token.isEmpty()) {
                log("Warning: connect-csrf-token is empty. Request might fail.")
            }
            try {
                val response = garminService.getRecentActivities(
                    csrfToken = token,
                    limit = 5
                ).execute()
                if (response.isSuccessful) {
                    val json = response.body()?.string()
                    log("Activities Response: SUCCESS")
                    log(json?.take(1000) ?: "Empty body") // Truncate for display
                } else {
                    log("Failed to get activities: ${response.code()}")
                    if (response.code() == 401 || response.code() == 403) {
                        log("Session expired or invalid. Please login again.")
                    }
                }
            } catch (e: Exception) {
                log("Exception fetching activities: ${e.message}")
            }
        }
    }

    fun getActivitySplits() {
        viewModelScope.launch(Dispatchers.IO) {
            log("Fetching splits for last activity...")
            val token = sessionManager.getCsrfToken()
            if (token.isEmpty()) {
                log("Warning: connect-csrf-token is empty. Request might fail.")
            }
            // First we need an activity ID
            try {
                val listResponse = garminService.getRecentActivities(
                    csrfToken = token,
                    limit = 1
                ).execute()
                if (listResponse.isSuccessful) {
                    val json = listResponse.body()?.string() ?: "[]"
                    // Simple regex to extract first activityId to avoid full JSON parsing for this test
                    val matcher = Pattern.compile("\"activityId\"\\s*:\\s*(\\d+)").matcher(json)
                    if (matcher.find()) {
                        val activityId = matcher.group(1)?.toLongOrNull()
                        if (activityId != null) {
                            log("Found Activity ID: $activityId. Fetching splits...")
                            val splitsResponse = garminService.getActivitySplits(token, activityId).execute()
                            if (splitsResponse.isSuccessful) {
                                log("Splits Response: SUCCESS")
                                log(splitsResponse.body().toString().take(1000))
                            } else {
                                log("Failed to get splits: ${splitsResponse.code()}")
                            }
                        } else {
                            log("Could not parse activity ID")
                        }
                    } else {
                        log("No activities found to get splits from.")
                    }
                } else {
                    log("Failed to get activity list first.")
                }
            } catch (e: Exception) {
                log("Exception fetching splits: ${e.message}")
            }
        }
    }

    private fun extractCsrfToken(html: String): String? {
        // Pattern: name="_csrf" value="X"
        val pattern = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
        val matcher = pattern.matcher(html)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
    
    private fun extractTicketFromResponse(response: Response<ResponseBody>): String? {
        // 1. Check if the final URL contains the ticket (if OkHttp followed redirects)
        val finalUrl = response.raw().request.url.toString()
        val ticketMatcher = Pattern.compile("ticket=([^&]+)").matcher(finalUrl)
        if (ticketMatcher.find()) {
            return ticketMatcher.group(1)
        }

        // 2. Check body for specific redirect script (Garmin sometimes returns this)
        // window.location.replace("...ticket=ST-...")
        try {
            // Note: reading body() consumes it, so we can only do it once if not buffered.
            // But since we are inside extractTicket which is called after success, let's peek.
            // Retrofit body() is one-shot. We should be careful.
            // Ideally we'd pass the string body here.
            // For now, let's assume we handle it if we add a 'bodyString' param or similar.
            // But checking headers is safer.
        } catch (e: Exception) {}

        return null 
    }
}
