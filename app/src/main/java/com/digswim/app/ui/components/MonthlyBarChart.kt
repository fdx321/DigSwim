package com.digswim.app.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digswim.app.ui.theme.NeonGreen
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun MonthlyBarChart(
    dailyDistances: List<Int>, // 28-31 days
    selectedMonth: YearMonth,
    modifier: Modifier = Modifier
) {
    Log.d("MonthlyBarChart", "Rendering for $selectedMonth. DailyDistances size: ${dailyDistances.size}")

    val textMeasurer = rememberTextMeasurer()
    val gridColor = Color.DarkGray.copy(alpha = 0.3f)
    val labelStyle = TextStyle(color = Color.Gray, fontSize = 10.sp)
    val noDataStyle = TextStyle(color = Color.Gray, fontSize = 14.sp)

    // Interaction State
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Dynamic Scale Calculation
    val daysInMonth = selectedMonth.lengthOfMonth()
    // Ensure we have data for all days, padding with 0 if necessary or if list is empty
    val safeDailyDistances = if (dailyDistances.size == daysInMonth) {
        dailyDistances
    } else {
        // If empty or mismatch, create a list of zeros
        List(daysInMonth) { index ->
            if (index < dailyDistances.size) dailyDistances[index] else 0
        }
    }

    val maxDistance = safeDailyDistances.maxOrNull() ?: 0
    val hasData = maxDistance > 0
    
    val yAxisMaxMeters = if (!hasData) {
        10000f
    } else {
        maxDistance.toFloat()
    }
    
    val steps = 5
    val stepValue = yAxisMaxMeters / (steps - 1)
    val yAxisLabels = List(steps) { i ->
        (i * stepValue) / 1000f // Convert to km
    }

    Box(
        modifier = modifier
            .pointerInput(safeDailyDistances) { 
                detectTapGestures { tapOffset ->
                    val paddingStartPx = 10.dp.toPx()
                    val paddingEndPx = 35.dp.toPx()
                    
                    val chartWidth = size.width - paddingStartPx - paddingEndPx
                    val barWidth = (chartWidth / daysInMonth) * 0.6f // Thinner bars for month
                    val barSpacing = (chartWidth - (barWidth * daysInMonth)) / daysInMonth
                    
                    if (tapOffset.x >= paddingStartPx && tapOffset.x <= size.width - paddingEndPx) {
                        val relativeX = tapOffset.x - paddingStartPx
                        
                        var foundIndex = -1
                        for (i in 0 until daysInMonth) {
                            val barLeft = (barSpacing / 2) + i * (barWidth + barSpacing)
                            val barRight = barLeft + barWidth
                            val slop = barSpacing / 2
                            if (relativeX >= barLeft - slop && relativeX <= barRight + slop) {
                                foundIndex = i
                                break
                            }
                        }
                        
                        if (foundIndex != -1) {
                            if (safeDailyDistances[foundIndex] > 0) {
                                selectedIndex = if (selectedIndex == foundIndex) null else foundIndex
                            } else {
                                selectedIndex = null
                            }
                        }
                    } else {
                        selectedIndex = null
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 35.dp, top = 20.dp, bottom = 20.dp)) {
            val width = size.width
            val height = size.height
            
            val xAxisHeight = 20.dp.toPx()
            val chartWidth = width 
            val chartHeight = height - xAxisHeight
            
            val barWidth = (chartWidth / daysInMonth) * 0.6f
            val barSpacing = (chartWidth - (barWidth * daysInMonth)) / daysInMonth
            
            // Draw Grid & Y-Axis Labels
            yAxisLabels.forEach { km ->
                val ratio = km * 1000f / yAxisMaxMeters
                val y = chartHeight - (ratio * chartHeight)
                
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f
                )
                
                val labelText = String.format("%.1f", km)
                val textLayout = textMeasurer.measure(labelText, labelStyle)
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(chartWidth + 8.dp.toPx(), y - textLayout.size.height / 2)
                )
            }
            
            // Draw X-Axis Labels (Selective)
            // Show: 01, 07, 14, 21, 28... Last Day
            // Or roughly every 7 days? 
            // User requested: "01 to 31, 30... depending on month"
            // Since we can't fit 31 labels, let's show key dates: 1, 8, 15, 22, 29... or just 1, 5, 10, 15, 20, 25, End
            val labelIndices = listOf(0, 4, 9, 14, 19, 24, daysInMonth - 1)
            
            labelIndices.forEach { index ->
                if (index < daysInMonth) {
                    val label = String.format("%02d", index + 1)
                    val x = (barSpacing / 2) + index * (barWidth + barSpacing)
                    val textLayout = textMeasurer.measure(label, labelStyle)
                    
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(x + (barWidth - textLayout.size.width) / 2, chartHeight + 8.dp.toPx())
                    )
                }
            }
            
            // Draw Bars
            if (hasData) {
                safeDailyDistances.forEachIndexed { index, meters ->
                    if (meters > 0) {
                        val barHeight = (meters / yAxisMaxMeters) * chartHeight
                        val x = (barSpacing / 2) + index * (barWidth + barSpacing)
                        val y = chartHeight - barHeight
                        
                        val isSelected = selectedIndex == index
                        val isAnySelected = selectedIndex != null
                        
                        val barColor = if (isAnySelected && !isSelected) {
                             NeonGreen.copy(alpha = 0.3f)
                        } else {
                             NeonGreen
                        }

                        val barRect = Rect(offset = Offset(x, y), size = Size(barWidth, barHeight))
                        val cornerRadius = CornerRadius(1.dp.toPx()) // Smaller radius for thin bars
                        val barPath = Path().apply {
                            addRoundRect(androidx.compose.ui.geometry.RoundRect(barRect, cornerRadius))
                        }

                        drawStripedBar(
                            drawScope = this,
                            path = barPath,
                            color = barColor,
                            rect = barRect
                        )
                        
                        if (isSelected) {
                            // Tooltip
                            val tooltipDistance = "${String.format("%.1f", meters / 1000f)}km"
                            val tooltipDate = selectedMonth.atDay(index + 1).format(DateTimeFormatter.ofPattern("MM/dd"))
                            
                            val distanceStyle = TextStyle(color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val dateStyle = TextStyle(color = Color.Gray, fontSize = 10.sp)
                            
                            val distanceLayout = textMeasurer.measure(tooltipDistance, distanceStyle)
                            val dateLayout = textMeasurer.measure(tooltipDate, dateStyle)
                            
                            val tooltipWidth = max(distanceLayout.size.width, dateLayout.size.width) + 16.dp.toPx()
                            val tooltipHeight = distanceLayout.size.height + dateLayout.size.height + 12.dp.toPx()
                            
                            var tooltipX = x + (barWidth - tooltipWidth) / 2
                            if (tooltipX < 0) tooltipX = 0f
                            if (tooltipX + tooltipWidth > width + 35.dp.toPx()) tooltipX = width - tooltipWidth
                            
                            val tooltipY = y - tooltipHeight - 8.dp.toPx()
                            
                            drawRoundRect(
                                color = Color(0xFF1E1E1E),
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                            drawRoundRect(
                                color = NeonGreen,
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipWidth, tooltipHeight),
                                cornerRadius = CornerRadius(4.dp.toPx()),
                                style = Stroke(width = 1.dp.toPx())
                            )
                            
                            drawText(
                                textLayoutResult = distanceLayout,
                                topLeft = Offset(tooltipX + (tooltipWidth - distanceLayout.size.width) / 2, tooltipY + 4.dp.toPx())
                            )
                            drawText(
                                textLayoutResult = dateLayout,
                                topLeft = Offset(tooltipX + (tooltipWidth - dateLayout.size.width) / 2, tooltipY + 4.dp.toPx() + distanceLayout.size.height)
                            )
                            
                            drawLine(
                                color = NeonGreen,
                                start = Offset(x + barWidth/2, y - 8.dp.toPx()),
                                end = Offset(x + barWidth/2, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }
        }
        
        if (!hasData) {
            androidx.compose.material3.Text(
                text = "本月无记录",
                style = noDataStyle,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

private fun drawStripedBar(
    drawScope: DrawScope,
    path: Path,
    color: Color,
    rect: Rect
) {
    drawScope.clipPath(path) {
        drawPath(path = path, color = color, style = Stroke(width = 1.dp.toPx()))
        val stripeWidth = 2.dp.toPx()
        val stripeGap = 4.dp.toPx()
        val startX = rect.left - rect.height 
        val endX = rect.right
        var currentX = startX
        while (currentX < endX + rect.height) {
            drawScope.drawLine(
                color = color,
                start = Offset(currentX, rect.bottom),
                end = Offset(currentX + rect.height, rect.top),
                strokeWidth = 1.dp.toPx()
            )
            currentX += (stripeWidth + stripeGap)
        }
    }
}
