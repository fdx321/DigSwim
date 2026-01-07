package com.digswim.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digswim.app.data.SwimRepository
import com.digswim.app.model.SwimActivityDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    private val swimRepository: SwimRepository
) : ViewModel() {

    private val _activityDetail = MutableStateFlow<UiState<SwimActivityDetail>>(UiState.Loading)
    val activityDetail: StateFlow<UiState<SwimActivityDetail>> = _activityDetail

    fun loadActivityDetail(activityId: String) {
        viewModelScope.launch {
            _activityDetail.value = UiState.Loading
            val detail = swimRepository.getActivityDetail(activityId)
            if (detail != null) {
                _activityDetail.value = UiState.Success(detail)
            } else {
                _activityDetail.value = UiState.Error("Failed to load activity details")
            }
        }
    }
}

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
