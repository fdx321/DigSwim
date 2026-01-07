package com.digswim.app.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarminSessionManager @Inject constructor() {
    private val _connectCsrfToken = MutableStateFlow<String>("")
    val connectCsrfToken: StateFlow<String> = _connectCsrfToken.asStateFlow()

    fun setCsrfToken(token: String) {
        _connectCsrfToken.value = token
    }

    fun getCsrfToken(): String {
        return _connectCsrfToken.value
    }
    
    fun hasSession(): Boolean {
        return _connectCsrfToken.value.isNotEmpty()
    }
}
