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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelectionBottomSheet(
    currentSelectedMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    var selectedYear by remember { mutableStateOf(currentSelectedMonth.year) }
    var selectedMonthValue by remember { mutableStateOf(currentSelectedMonth.monthValue) }

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
                        onConfirm(YearMonth.of(selectedYear, selectedMonthValue))
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
                // Year Wheel (Left, 50% width)
                Box(modifier = Modifier.weight(0.5f)) {
                    YearWheelPicker(
                        initialYear = selectedYear,
                        onYearSelected = { selectedYear = it }
                    )
                }

                // Month Wheel (Right, 50% width)
                Box(modifier = Modifier.weight(0.5f)) {
                    MonthWheelPicker(
                        initialMonth = selectedMonthValue,
                        selectedYear = selectedYear,
                        onMonthSelected = { selectedMonthValue = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthWheelPicker(
    initialMonth: Int,
    selectedYear: Int, // Add this parameter
    onMonthSelected: (Int) -> Unit
) {
    val currentYear = java.time.LocalDate.now().year
    val currentMonth = java.time.LocalDate.now().monthValue
    
    // If selected year is current year, limit months to current month
    val maxMonth = if (selectedYear == currentYear) currentMonth else 12
    val months = (1..maxMonth).toList()
    
    val listState = rememberLazyListState()
    
    // Initial scroll
    LaunchedEffect(Unit) {
        val index = months.indexOf(initialMonth)
        if (index != -1) {
            listState.scrollToItem(index)
        }
    }
    
    // Also scroll if selectedYear changes and current month is out of bounds
    // Or if the list content changes significantly
    LaunchedEffect(selectedYear, maxMonth) {
         if (initialMonth > maxMonth && months.isNotEmpty()) {
             // If previously selected month is now invalid (e.g. switched to current year, and old month was Dec but now is May)
             // Snap to latest available month
             listState.scrollToItem(months.lastIndex)
             onMonthSelected(months.last())
         } else {
             // Ensure visible
             val index = months.indexOf(initialMonth)
             if (index != -1 && !listState.isScrollInProgress) {
                  listState.scrollToItem(index)
             }
         }
    }
    
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    LaunchedEffect(listState, months) { // Re-run when months list changes
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect {
                if (months.isEmpty()) return@collect
                
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
                        val month = months.getOrNull(it.index)
                        if (month != null) {
                            onMonthSelected(month)
                        }
                    }
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
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
            items(months) { month ->
                val isSelected = month == initialMonth // Visual feedback handled by launched effect state sync might be delayed, but close enough
                
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${month}月",
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
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
