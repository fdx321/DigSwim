package com.digswim.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.draw.rotate
import com.digswim.app.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digswim.app.model.SwimActivity
import com.digswim.app.model.SwimType
import com.digswim.app.ui.components.MonthSelectionBottomSheet
import com.digswim.app.ui.components.MonthlyBarChart
import com.digswim.app.ui.components.WeekSelectionBottomSheet
import com.digswim.app.ui.components.WeeklyBarChart
import com.digswim.app.ui.components.YearSelectionBottomSheet
import com.digswim.app.ui.components.YearlyBarChart
import com.digswim.app.ui.theme.NeonGreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun HomeOverviewScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToMonthlyReport: (YearMonth) -> Unit = {},
    onNavigateToActivityDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    // Data from ViewModel
    val currentTab by viewModel.selectedTab.collectAsState()
    
    val currentWeekSummary by viewModel.currentWeeklySummary.collectAsState()
    val currentMonthlySummary by viewModel.currentMonthlySummary.collectAsState()
    val currentYearlySummary by viewModel.currentYearlySummary.collectAsState()
    val weeklyActivities by viewModel.weeklyActivities.collectAsState()
    
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    
    val selectedWeekStart by viewModel.selectedWeekStart.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    
    // Determine which list to show based on Tab
    // "Recent" list logic: 
    // Week Tab -> Show activities <= selectedWeekEnd
    // Month Tab -> Show activities <= selectedMonthEnd
    // Year Tab -> Show activities <= end of selected year
    val filteredActivities = when(currentTab) {
        OverviewTab.Week -> {
            val selectedWeekEnd = selectedWeekStart.plusDays(6)
            weeklyActivities.filter { 
                !it.startTime.toLocalDate().isAfter(selectedWeekEnd) 
            }.sortedByDescending { it.startTime }
        }
        OverviewTab.Month -> {
            val selectedMonthStart = selectedMonth.atDay(1)
            val selectedMonthEnd = selectedMonth.atEndOfMonth()
            weeklyActivities.filter {
                val date = it.startTime.toLocalDate()
                !date.isBefore(selectedMonthStart) && !date.isAfter(selectedMonthEnd)
            }.sortedByDescending { it.startTime }
        }
        OverviewTab.Year -> {
            // Filter by year
            weeklyActivities.filter {
                it.startTime.year == selectedYear
            }.sortedByDescending { it.startTime }
        }
        else -> emptyList() // TODO: Implement Total
    }

    // Group activities by week for Month view
    // Group activities by Month for Year view
    val groupedActivitiesMonth = remember(filteredActivities, currentTab) {
        if (currentTab == OverviewTab.Month) {
             filteredActivities.groupBy { 
                it.startTime.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) 
            }.toSortedMap(Comparator.reverseOrder())
        } else {
            emptyMap()
        }
    }
    
    val groupedActivitiesYear = remember(filteredActivities, currentTab) {
        if (currentTab == OverviewTab.Year) {
            filteredActivities.groupBy {
                YearMonth.from(it.startTime)
            }.toSortedMap(Comparator.reverseOrder())
        } else {
            emptyMap()
        }
    }

    var showSelector by remember { mutableStateOf(false) }

    if (showSelector) {
        if (currentTab == OverviewTab.Week) {
            WeekSelectionBottomSheet(
                currentSelectedDate = selectedWeekStart,
                onDismiss = { showSelector = false },
                onConfirm = { newDate -> 
                    viewModel.selectWeekByDate(newDate)
                }
            )
        } else if (currentTab == OverviewTab.Month) {
            MonthSelectionBottomSheet(
                currentSelectedMonth = selectedMonth,
                onDismiss = { showSelector = false },
                onConfirm = { newMonth ->
                    viewModel.selectMonth(newMonth)
                }
            )
        } else if (currentTab == OverviewTab.Year) {
            YearSelectionBottomSheet(
                currentYear = selectedYear,
                onDismiss = { showSelector = false },
                onConfirm = { newYear ->
                    viewModel.selectYear(newYear)
                }
            )
        }
    }

    // Load More Logic
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset
                (lastVisibleItem.index + 1 == layoutInfo.totalItemsCount) &&
                (lastVisibleItem.offset + lastVisibleItem.size <= viewportHeight)
            }
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !isLoadingMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "记录",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                
                // Sync Button
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(enabled = !isSyncing) {
                            viewModel.syncData(
                                onAuthError = {
                                    Toast.makeText(context, "请先在“我的”页面设置 Garmin 账号", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing)
                        ),
                        label = "rotation"
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sync),
                        contentDescription = "Sync",
                        tint = if (isSyncing) NeonGreen else Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (isSyncing) angle else 0f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSyncing) "同步中..." else "数据同步",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSyncing) NeonGreen else Color.Gray
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Time Segment Control (Tab Row)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        OverviewTab.Week to "周",
                        OverviewTab.Month to "月",
                        OverviewTab.Year to "年",
                        OverviewTab.Total to "总"
                    )
                    
                    tabs.forEach { (tab, label) ->
                        if (currentTab == tab) {
                            // Selected Tab (Dropdown Style)
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                    .clickable { showSelector = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val displayText = when(tab) {
                                        OverviewTab.Week -> if (viewModel.isCurrentWeek(selectedWeekStart)) "本周" else selectedWeekStart.format(DateTimeFormatter.ofPattern("MM.dd"))
                                        OverviewTab.Month -> if (viewModel.isCurrentMonth(selectedMonth)) "本月" else selectedMonth.format(DateTimeFormatter.ofPattern("yyyy.MM"))
                                        OverviewTab.Year -> if (viewModel.isCurrentYear(selectedYear)) "今年" else selectedYear.toString()
                                        else -> label // Placeholder for Total
                                    }
                                    
                                    Text(
                                        text = displayText,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            // Unselected Tab (Plain Text)
                            Text(
                                text = label,
                                color = Color.Gray,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clickable { viewModel.selectTab(tab) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. Chart Area
            item {
                if (currentTab == OverviewTab.Total) {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "敬请期待",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    AnimatedContent(targetState = currentTab, label = "chart_transition") { targetTab ->
                        when (targetTab) {
                            OverviewTab.Week -> {
                                WeeklyBarChart(
                                    dailyDistances = currentWeekSummary?.dailyDistances ?: List(7) { 0 },
                                    weekStartDate = selectedWeekStart,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                )
                            }
                            OverviewTab.Month -> {
                                MonthlyBarChart(
                                    dailyDistances = currentMonthlySummary?.dailyDistances ?: emptyList(),
                                    selectedMonth = selectedMonth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                )
                            }
                            OverviewTab.Year -> {
                                YearlyBarChart(
                                    monthlyDistances = currentYearlySummary?.monthlyDistances ?: List(12) { 0 },
                                    year = selectedYear,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                )
                            }
                            else -> {
                                Box(modifier = Modifier.height(250.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Stats Grid
            if (currentTab != OverviewTab.Total) {
                item {
                    // Switch stats source based on tab
                    val stats = when(currentTab) {
                    OverviewTab.Week -> {
                        val s = currentWeekSummary
                        listOf(
                            s?.totalDurationSeconds ?: 0L,
                            s?.totalDistanceMeters ?: 0,
                            s?.avgPaceSecondsPer100m ?: 0,
                            s?.avgHeartRate ?: 0,
                            s?.swimCount ?: 0,
                            s?.totalCalories ?: 0,
                            s?.avgSwolf ?: 0
                        )
                    }
                    OverviewTab.Month -> {
                        val s = currentMonthlySummary
                        listOf(
                            s?.totalDurationSeconds ?: 0L,
                            s?.totalDistanceMeters ?: 0,
                            s?.avgPaceSecondsPer100m ?: 0,
                            s?.avgHeartRate ?: 0,
                            s?.swimCount ?: 0,
                            s?.totalCalories ?: 0,
                            s?.avgSwolf ?: 0
                        )
                    }
                    OverviewTab.Year -> {
                        val s = currentYearlySummary
                        listOf(
                            s?.totalDurationSeconds ?: 0L,
                            s?.totalDistanceMeters ?: 0,
                            s?.avgPaceSecondsPer100m ?: 0,
                            s?.avgHeartRate ?: 0,
                            s?.swimCount ?: 0,
                            s?.totalCalories ?: 0,
                            s?.avgSwolf ?: 0
                        )
                    }
                    else -> listOf(0L, 0, 0, 0, 0, 0, 0)
                }

                val duration = stats[0] as Long
                val distance = stats[1] as Int
                val pace = stats[2] as Int
                val hr = stats[3] as Int
                val count = stats[4] as Int
                val cals = stats[5] as Int
                val swolf = stats[6] as Int

                // Row 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(
                        value = formatDuration(duration as Long),
                        label = "游泳时长"
                    )
                    StatItem(
                        value = String.format("%.1f", (distance as Int) / 1000f),
                        label = "总距离(km)"
                    )
                    StatItem(
                        value = formatPace(pace as Int),
                        label = "平均配速"
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Row 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(
                        value = "$hr",
                        label = "平均心率"
                    )
                    StatItem(
                        value = "$count",
                        label = "游泳次数"
                    )
                    StatItem(
                        value = "$cals",
                        label = "卡路里(kcal)"
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Row 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(
                        value = "$swolf",
                        label = "平均swolf"
                    )
                    Spacer(modifier = Modifier.width(100.dp))
                    Spacer(modifier = Modifier.width(100.dp))
                }
                
                Spacer(modifier = Modifier.height(30.dp))
            }
            }

            // 4. "Recent" Header and List
            if (currentTab != OverviewTab.Total) {
                item {
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerText = when(currentTab) {
                        OverviewTab.Month -> selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月"))
                        OverviewTab.Year -> "${selectedYear}年"
                        else -> "最近"
                    }
                    
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (currentTab == OverviewTab.Month && currentMonthlySummary?.swimCount ?: 0 > 0) {
                         IconButton(
                             onClick = { onNavigateToMonthlyReport(selectedMonth) },
                             modifier = Modifier
                                .size(36.dp)
                         ) {
                             Icon(
                                 imageVector = Icons.Default.Timeline,
                                 contentDescription = "Monthly Report",
                                 tint = NeonGreen,
                                 modifier = Modifier.fillMaxSize()
                             )
                         }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (currentTab == OverviewTab.Month) {
                groupedActivitiesMonth.forEach { (weekStart, activities) ->
                    item(key = weekStart) {
                        MonthlyWeekItem(
                            weekStart = weekStart,
                            activities = activities,
                            onActivityClick = onNavigateToActivityDetail
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else if (currentTab == OverviewTab.Year) {
                groupedActivitiesYear.forEach { (yearMonth, activities) ->
                    item(key = yearMonth) {
                        YearlyMonthItem(
                            yearMonth = yearMonth,
                            activities = activities,
                            onActivityClick = onNavigateToActivityDetail
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else if (currentTab == OverviewTab.Week) {
                items(filteredActivities) { activity ->
                    ActivityRow(
                        activity = activity,
                        onClick = { onNavigateToActivityDetail(activity.id) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            item {
                if (isLoadingMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
            }
        }
    }
}

@Composable
fun YearlyMonthItem(
    yearMonth: YearMonth,
    activities: List<SwimActivity>,
    onActivityClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val totalDistance = activities.sumOf { it.distanceMeters }
    val count = activities.size
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Month + Stats
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Month Text "06月" with Gradient
                    Text(
                        text = yearMonth.format(DateTimeFormatter.ofPattern("MM月")),
                        style = TextStyle(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFE0E0E0), Color(0xFF666666))
                            ),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Stats Row "18次 · 190.95km v"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${count}次",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = " · ",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (totalDistance >= 1000) 
                                String.format("%.2fkm", totalDistance / 1000f)
                            else 
                                "${totalDistance}m",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Right Column: Calendar Matrix
                MonthCalendarMatrix(
                    yearMonth = yearMonth,
                    activeDates = activities.map { it.startTime.toLocalDate() }.toSet(),
                    modifier = Modifier.size(width = 100.dp, height = 60.dp)
                )
            }
            
            // Expanded Content
            AnimatedVisibility(visible = expanded) {
                Column {
                    // Separator line
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                     activities.forEachIndexed { index, activity ->
                         ActivityRow(activity, onClick = { onActivityClick(activity.id) })
                         if (index < activities.size - 1) {
                             Spacer(modifier = Modifier.height(12.dp))
                         }
                     }
                }
            }
        }
    }
}

@Composable
fun MonthCalendarMatrix(
    yearMonth: YearMonth,
    activeDates: Set<LocalDate>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value // 1 (Mon) - 7 (Sun)
        
        // Grid config
        val cols = 7
        val rows = 6 // Max rows needed for a month
        
        // Calculate cell size
        // Use fixed spacing or dynamic? Let's use dynamic to fit width
        val spacing = 4.dp.toPx()
        val cellWidth = (size.width - (cols - 1) * spacing) / cols
        // Keep cells square-ish or rectangular? Square is better for dots.
        // Let's use small squares.
        val cellSize = minOf(cellWidth, (size.height - (rows - 1) * spacing) / rows)
        
        val cornerRadius = 2.dp.toPx()
        
        for (day in 1..daysInMonth) {
            // Calculate grid position
            // Day 1 position depends on firstDayOfWeek.
            // Standard calendar: Mon=0, Sun=6
            // dayOfWeek.value: Mon=1, Sun=7 -> Convert to 0-6 index
            val startOffset = firstDayOfWeek - 1 
            val totalIndex = startOffset + (day - 1)
            
            val col = totalIndex % 7
            val row = totalIndex / 7
            
            if (row >= rows) break // Should not happen for standard months
            
            val x = col * (cellSize + spacing) + (size.width - (cols * cellSize + (cols-1)*spacing)) // Right align? Or Left? Let's right align to match design
            val y = row * (cellSize + spacing)
            
            val date = yearMonth.atDay(day)
            val isActive = activeDates.contains(date)
            
            val color = if (isActive) NeonGreen else Color(0xFF333333)
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}

// ... StatItem, ActivityRow, formatDuration, formatPace helper functions remain same
@Composable
fun StatItem(value: String, label: String) {
    Column(modifier = Modifier.width(100.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ActivityRow(activity: SwimActivity, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swim_logo),
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (activity.type == SwimType.POOL) "泳池游泳" else "公开水域",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (activity.distanceMeters >= 1000) 
                            String.format("%.2f", activity.distanceMeters / 1000f) 
                        else 
                            activity.distanceMeters.toString(),
                        color = NeonGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (activity.distanceMeters >= 1000) "km" else "m",
                        color = NeonGreen,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Row {}
                Text(
                    text = activity.startTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

fun formatPace(secondsPer100m: Int): String {
    val m = secondsPer100m / 60
    val s = secondsPer100m % 60
    return String.format("%d'%02d\"", m, s)
}

fun formatDurationDecimal(seconds: Long): String {
    val hours = seconds / 3600.0
    return String.format("%.2f小时", hours)
}

@Composable
fun MonthlyWeekItem(
    weekStart: LocalDate,
    activities: List<SwimActivity>,
    onActivityClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val weekEnd = weekStart.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("MM.dd")
    // Added spaces around hyphen as per visual style
    val dateRange = "${weekStart.format(formatter)} - ${weekEnd.format(formatter)}"
    
    val totalDistance = activities.sumOf { it.distanceMeters }
    val totalSeconds = activities.sumOf { it.durationSeconds }
    val count = activities.size
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Gradient and Italic style for date range
                    Text(
                        text = dateRange,
                        style = TextStyle(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFE0E0E0), Color(0xFF888888))
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${count}次",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = " · ",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                         Text(
                            text = formatDurationDecimal(totalSeconds),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Text(
                    text = if (totalDistance >= 1000) 
                        String.format("%.2fkm", totalDistance / 1000f)
                    else 
                        "${totalDistance}m",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic 
                )
            }
            
            // Arrow logic moved to the row above
            // Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { ... } removed

            AnimatedVisibility(visible = expanded) {
                Column {
                    // Separator line
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                     activities.forEachIndexed { index, activity ->
                         ActivityRow(activity, onClick = { onActivityClick(activity.id) })
                         if (index < activities.size - 1) {
                             Spacer(modifier = Modifier.height(12.dp))
                         }
                     }
                }
            }
        }
    }
}
