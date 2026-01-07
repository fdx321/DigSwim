package com.digswim.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digswim.app.data.local.UserPreferences
import com.digswim.app.data.local.UserPreferencesRepository
import com.digswim.app.data.local.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userPreferencesRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile("加载中...", "...")
        )
        
    val garminEmail: kotlinx.coroutines.flow.Flow<String?> = userPreferences.garminEmail
    val garminPassword: kotlinx.coroutines.flow.Flow<String?> = userPreferences.garminPassword

    fun updateProfile(nickname: String, bio: String, avatarUrl: String?) {
        val currentPoolLength = userProfile.value.poolLength
        val currentGender = userProfile.value.gender
        viewModelScope.launch {
            userPreferencesRepository.updateUserProfile(nickname, bio, avatarUrl, currentGender, currentPoolLength)
        }
    }
    
    fun saveGarminCredentials(email: String, pass: String) {
        viewModelScope.launch {
            userPreferences.saveGarminCredentials(email, pass)
        }
    }
}
