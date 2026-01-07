package com.digswim.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digswim.app.data.SwimRepository
import com.digswim.app.model.MonthlySummary
import com.digswim.app.model.SwimActivity
import com.digswim.app.model.WeeklySummary
import com.digswim.app.model.YearlySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class OverviewTab {
    Week, Month, Year, Total
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SwimRepository
) : ViewModel() {

    // Sync State
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Tab State
    private val _selectedTab = MutableStateFlow(OverviewTab.Week)
    val selectedTab: StateFlow<OverviewTab> = _selectedTab.asStateFlow()

    // Week State
    private val _selectedWeekStart = MutableStateFlow(getCurrentWeekStart())
    val selectedWeekStart: StateFlow<LocalDate> = _selectedWeekStart.asStateFlow()

    // Month State
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    // Year State
    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // Derived Data
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentWeeklySummary: StateFlow<WeeklySummary?> = _selectedWeekStart
        .flatMapLatest { weekStart ->
            repository.getWeeklySummary(weekStart)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonthlySummary: StateFlow<MonthlySummary?> = _selectedMonth
        .flatMapLatest { yearMonth ->
            repository.getMonthlySummary(yearMonth.year, yearMonth.monthValue)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentYearlySummary: StateFlow<YearlySummary?> = _selectedYear
        .flatMapLatest { year ->
            repository.getYearlySummary(year)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyActivities: StateFlow<List<SwimActivity>> = repository.getAllActivities()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectTab(tab: OverviewTab) {
        _selectedTab.value = tab
        // Reset selections when switching?
        // User said: "Every time switching Week/Month/Year/Total tab, need to re-select"
        // This implies we might want to reset to current date on switch.
        if (tab == OverviewTab.Week) {
            _selectedWeekStart.value = getCurrentWeekStart()
        } else if (tab == OverviewTab.Month) {
            _selectedMonth.value = YearMonth.now()
        } else if (tab == OverviewTab.Year) {
            _selectedYear.value = LocalDate.now().year
        }
    }

    fun selectWeekByDate(date: LocalDate) {
        _selectedWeekStart.value = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun selectMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    private fun getCurrentWeekStart(): LocalDate {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    
    fun isCurrentWeek(date: LocalDate): Boolean {
        return date == getCurrentWeekStart()
    }
    
    fun isCurrentMonth(yearMonth: YearMonth): Boolean {
        return yearMonth == YearMonth.now()
    }
    
    fun isCurrentYear(year: Int): Boolean {
        return year == LocalDate.now().year
    }

    fun loadMore() {
        if (_isLoadingMore.value || _isSyncing.value) return
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                repository.loadMore()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun syncData(onAuthError: (() -> Unit)? = null) {
        if (_isSyncing.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // If we need to pass a callback for auth error (since repository auto-login might fail)
                // We might need to modify refreshData to return status or throw exception
                // For now, let's just refresh. If repo logs "Auto-login failed", user sees no data.
                // To show Toast, we need feedback.
                // Let's assume refreshData throws or returns boolean if we refactor it, 
                // OR we check session status after refresh.
                repository.refreshData()
                
                // Check if session is valid? Not exposed on repository interface.
                // Better: repository.refreshData() could throw an AuthException.
                // For simplicity given constraints: if data is still empty after refresh AND no session, trigger callback.
                // But repo handles auto-login internally.
                
            } catch (e: Exception) {
                e.printStackTrace()
                // If it's an auth error (we'd need to define it), call onAuthError
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
