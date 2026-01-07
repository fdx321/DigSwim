package com.digswim.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digswim.app.ui.theme.NeonGreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekSelectionBottomSheet(
    currentSelectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    // Internal state
    var selectedYear by remember { mutableStateOf(currentSelectedDate.year) }
    var selectedWeekStart by remember { mutableStateOf(currentSelectedDate) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 30.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择时间",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "确定",
                    color = Color.White,
                    modifier = Modifier.clickable {
                        onConfirm(selectedWeekStart)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Wheel Pickers Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year Wheel (Left, 30% width)
                Box(modifier = Modifier.weight(0.3f)) {
                    YearWheelPicker(
                        initialYear = selectedYear,
                        onYearSelected = { year ->
                            if (year != selectedYear) {
                                selectedYear = year
                                // When year changes, auto-select the latest week of that year
                                // to prevent invalid state (e.g. current week doesn't exist in that year)
                                val weeks = generateWeeksForYear(year)
                                if (weeks.isNotEmpty()) {
                                    selectedWeekStart = weeks.first().first
                                }
                            }
                        }
                    )
                }

                // Week Wheel (Right, 70% width)
                Box(modifier = Modifier.weight(0.7f)) {
                    WeekWheelPicker(
                        year = selectedYear,
                        currentSelectedDate = selectedWeekStart,
                        onWeekSelected = { weekStart ->
                            selectedWeekStart = weekStart
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YearWheelPicker(
    initialYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val currentYear = LocalDate.now().year
    val startYear = 2020
    val years = (startYear..currentYear).toList()
    val listState = rememberLazyListState()
    
    // Initial scroll
    LaunchedEffect(Unit) {
        val index = years.indexOf(initialYear)
        if (index != -1) {
            listState.scrollToItem(index)
        }
    }
    
    // Snap behavior
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // Detect center item via snapshotFlow
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect {
                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isNotEmpty()) {
                    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                    val centerOffset = (viewportHeight / 2) + layoutInfo.viewportStartOffset
                    
                    val centerItem = visibleItemsInfo.minByOrNull { item -> 
                        val itemCenter = item.offset + (item.size / 2)
                        abs(itemCenter - centerOffset)
                    }
                    
                    centerItem?.let {
                        val year = years.getOrNull(it.index)
                        if (year != null) {
                            onYearSelected(year)
                        }
                    }
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Highlight Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF333333), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 80.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(years) { year ->
                val isSelected = year == initialYear
                
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$year 年",
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.alpha(if (isSelected) 1f else 1f)
                    )
                }
            }
        }
        
        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1E1E), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1E1E1E))))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekWheelPicker(
    year: Int,
    currentSelectedDate: LocalDate,
    onWeekSelected: (LocalDate) -> Unit
) {
    val weeks = remember(year) { generateWeeksForYear(year) }
    val listState = rememberLazyListState()

    // Sync scroll with selection
    LaunchedEffect(currentSelectedDate, weeks) {
        val index = weeks.indexOfFirst { it.first == currentSelectedDate }
        if (index != -1) {
            // Only scroll if not already visible/close to avoid fighting with user scroll
            // But for a wheel picker, we usually want to center it.
            // Check if currently scrolling? No, just snap.
            if (!listState.isScrollInProgress) {
                listState.scrollToItem(index)
            }
        }
    }

    // Snap behavior
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Detect center item
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect {
                // Skip detection if list is empty
                if (weeks.isEmpty()) return@collect

                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isNotEmpty()) {
                    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                    val centerOffset = (viewportHeight / 2) + layoutInfo.viewportStartOffset
                    
                    val centerItem = visibleItemsInfo.minByOrNull { item -> 
                        val itemCenter = item.offset + (item.size / 2)
                        abs(itemCenter - centerOffset)
                    }
                    
                    centerItem?.let {
                        val week = weeks.getOrNull(it.index)
                        if (week != null) {
                            onWeekSelected(week.first)
                        }
                    }
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Highlight Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF333333), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 80.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(weeks) { (weekStart, label) ->
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = weekStart == currentSelectedDate
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1E1E), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1E1E1E))))
        )
    }
}


private fun generateWeeksForYear(year: Int): List<Pair<LocalDate, String>> {
    val weeks = mutableListOf<Pair<LocalDate, String>>()
    var date = LocalDate.of(year, 1, 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    
    // If the first week's majority is in previous year, start from next week
    // But standard is: if Jan 1 is in a week, that's week 1? 
    // Let's stick to current logic but ensuring we cover the year.
    // If date is in previous year completely? No, with(...) ensures it's near Jan 1.
    // Logic from before:
    if (date.year < year && date.plusDays(6).year < year) {
        date = date.plusWeeks(1)
    }

    val today = LocalDate.now()
    val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastWeekStart = thisWeekStart.minusWeeks(1)
    
    // Only use relative labels (本周/上周) if we are viewing the current year
    val isCurrentYear = year == today.year

    while (date.year <= year || (date.year == year + 1 && date.dayOfYear < 7)) {
        // Do not include future weeks
        if (date.isAfter(today)) break
        
        val label = if (isCurrentYear && date == thisWeekStart) {
            "本周"
        } else if (isCurrentYear && date == lastWeekStart) {
            "上周"
        } else {
            val end = date.plusDays(6)
            "${date.format(DateTimeFormatter.ofPattern("MM.dd"))}-${end.format(DateTimeFormatter.ofPattern("MM.dd"))}"
        }

        weeks.add(date to label)
        date = date.plusWeeks(1)
        
        // Safety break
        if (date.year > year + 1) break
    }
    return weeks.reversed()
}
