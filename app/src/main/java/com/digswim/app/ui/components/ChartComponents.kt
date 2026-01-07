package com.digswim.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digswim.app.model.MetricPoint
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Configuration for the Chart's Axis.
 */
data class AxisConfig(
    val showXAxis: Boolean = true,
    val showYAxis: Boolean = true,
    val showGrid: Boolean = true,
    val yAxisPosition: AxisPosition = AxisPosition.Right,
    val invertYAxis: Boolean = false, // Useful for Pace (lower is better/higher)
    val tickCountY: Int = 5,
    val tickCountX: Int = 5,
    val yAxisFormatter: (Double) -> String = { it.toInt().toString() },
    val xAxisFormatter: (Double) -> String = { it.toInt().toString() }
)

enum class AxisPosition { Left, Right }

/**
 * Visual styling for the Chart.
 */
data class ChartStyle(
    val lineColor: Color = Color(0xFFFF9800),
    val lineGradient: Brush? = null,
    val lineWidth: Dp = 2.dp,
    val gridColor: Color = Color.Gray.copy(alpha = 0.3f),
    val axisTextColor: Color = Color.Gray,
    val axisTextSize: TextStyle = TextStyle(fontSize = 10.sp, color = Color.Gray),
    val backgroundColor: Color = Color.Transparent
)

/**
 * A flexible Line Chart component using Jetpack Compose Canvas.
 */
@Composable
fun DigSwimLineChart(
    dataPoints: List<MetricPoint>,
    modifier: Modifier = Modifier,
    axisConfig: AxisConfig = AxisConfig(),
    style: ChartStyle = ChartStyle()
) {
    if (dataPoints.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    // 1. Calculate Bounds
    // Filter invalid points just in case
    val validPoints = dataPoints.filter { it.yValue > 0 }
    if (validPoints.isEmpty()) return

    val minX = dataPoints.minOf { it.xValue }
    val maxX = dataPoints.maxOf { it.xValue }
    
    val minYRaw = validPoints.minOf { it.yValue }
    val maxYRaw = validPoints.maxOf { it.yValue }

    // Add padding to Y range so lines don't touch top/bottom exactly
    val rangeY = maxYRaw - minYRaw
    // If flat line, add arbitrary padding
    val paddingY = if (rangeY == 0.0) maxYRaw * 0.1 else rangeY * 0.1
    
    // Calculate nice ticks for Y
    val minY = (minYRaw - paddingY).coerceAtLeast(0.0)
    val maxY = maxYRaw + paddingY

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Define drawing area (leaving space for axis labels)
            // Measure sample text to determine padding
            val yLabelWidth = measureMaxLabelWidth(minY, maxY, axisConfig.yAxisFormatter, textMeasurer, style.axisTextSize)
            val xLabelHeight = 20.dp.toPx() // Approximate height

            val chartAreaLeft = if (axisConfig.showYAxis && axisConfig.yAxisPosition == AxisPosition.Left) yLabelWidth + 8.dp.toPx() else 0f
            val chartAreaRight = if (axisConfig.showYAxis && axisConfig.yAxisPosition == AxisPosition.Right) width - yLabelWidth - 8.dp.toPx() else width
            val chartAreaTop = 10.dp.toPx() // Top padding
            val chartAreaBottom = if (axisConfig.showXAxis) height - xLabelHeight else height
            
            val chartWidth = chartAreaRight - chartAreaLeft
            val chartHeight = chartAreaBottom - chartAreaTop

            // 2. Draw Grid & Y-Axis Labels
            if (axisConfig.showGrid || axisConfig.showYAxis) {
                drawYAxisGridAndLabels(
                    minY, maxY, axisConfig, style, textMeasurer,
                    chartAreaLeft, chartAreaRight, chartAreaTop, chartHeight
                )
            }

            // 3. Draw X-Axis Labels
            if (axisConfig.showXAxis) {
                drawXAxisLabels(
                    minX, maxX, axisConfig, style, textMeasurer,
                    chartAreaLeft, chartAreaRight, chartAreaBottom
                )
            }

            // 4. Draw Data Line
            drawDataLine(
                dataPoints, minX, maxX, minY, maxY,
                chartAreaLeft, chartAreaTop, chartWidth, chartHeight,
                axisConfig.invertYAxis, style
            )
        }
    }
}

private fun DrawScope.drawYAxisGridAndLabels(
    minY: Double,
    maxY: Double,
    config: AxisConfig,
    style: ChartStyle,
    textMeasurer: TextMeasurer,
    left: Float,
    right: Float,
    top: Float,
    height: Float
) {
    val steps = config.tickCountY
    val stepValue = (maxY - minY) / (steps - 1)

    for (i in 0 until steps) {
        val value = minY + (i * stepValue)
        // Y-coordinate: 0 at top. 
        // Value mapping: value -> y
        // If normal: max is top (0), min is bottom (height)
        // If inverted: min is top (0), max is bottom (height)
        
        // However, usually we draw ticks from bottom to top or top to bottom.
        // Let's just calculate y position based on value.
        // Note: The visual Y position should match the data drawing logic.
        
        // Data logic:
        // Normal: y = height - ((val - min) / range * height) + top
        // Inverted: y = ((val - min) / range * height) + top
        
        val normalizedY = if (config.invertYAxis) {
            (value - minY) / (maxY - minY)
        } else {
            1 - (value - minY) / (maxY - minY)
        }
        
        val yPos = top + (normalizedY * height).toFloat()

        // Draw Grid Line
        if (config.showGrid) {
            drawLine(
                color = style.gridColor,
                start = Offset(left, yPos),
                end = Offset(right, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // Draw Label
        if (config.showYAxis) {
            val label = config.yAxisFormatter(value)
            val textLayout = textMeasurer.measure(label, style.axisTextSize)
            
            val textX = if (config.yAxisPosition == AxisPosition.Left) {
                left - textLayout.size.width - 4.dp.toPx()
            } else {
                right + 4.dp.toPx()
            }
            
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(textX, yPos - textLayout.size.height / 2)
            )
        }
    }
}

private fun DrawScope.drawXAxisLabels(
    minX: Double,
    maxX: Double,
    config: AxisConfig,
    style: ChartStyle,
    textMeasurer: TextMeasurer,
    left: Float,
    right: Float,
    bottom: Float
) {
    val steps = config.tickCountX
    val stepValue = (maxX - minX) / (steps - 1)

    for (i in 0 until steps) {
        val value = minX + (i * stepValue)
        val fraction = i.toFloat() / (steps - 1)
        val xPos = left + (fraction * (right - left))

        val label = config.xAxisFormatter(value)
        val textLayout = textMeasurer.measure(label, style.axisTextSize)
        
        // Align text center to tick
        // First/Last labels might need clamping to not go off-screen
        var drawX = xPos - textLayout.size.width / 2
        
        // Simple clamping
        if (i == 0) drawX = left
        if (i == steps - 1) drawX = right - textLayout.size.width

        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(drawX, bottom + 4.dp.toPx())
        )
    }
}

private fun DrawScope.drawDataLine(
    points: List<MetricPoint>,
    minX: Double,
    maxX: Double,
    minY: Double,
    maxY: Double,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    invertY: Boolean,
    style: ChartStyle
) {
    val path = Path()
    var isFirst = true
    
    val rangeX = maxX - minX
    val rangeY = maxY - minY

    points.forEach { point ->
        if (point.yValue <= 0) return@forEach // Skip gaps

        val x = left + ((point.xValue - minX) / rangeX * width).toFloat()
        
        // Y Calculation
        val normalizedY = if (invertY) {
            (point.yValue - minY) / rangeY // Higher value (slower pace) -> Higher Y (Bottom)
             // Wait.
             // If InvertY is TRUE (Pace):
             // We want Lower Value (Faster) at TOP (y=0 relative to chart area).
             // We want Higher Value (Slower) at BOTTOM (y=height).
             // So: (value - min) / range => 0..1. 0 is min, 1 is max.
             // If val = min (Fastest), normalized = 0. y = 0 + top. Correct (Top).
             // If val = max (Slowest), normalized = 1. y = height + top. Correct (Bottom).
             (point.yValue - minY) / rangeY
        } else {
            // Normal (HR): Higher Value at TOP.
            // If val = max, normalized should be 0 (Top).
            // If val = min, normalized should be 1 (Bottom).
            1 - (point.yValue - minY) / rangeY
        }
        
        val y = top + (normalizedY * height).toFloat()

        if (isFirst) {
            path.moveTo(x, y)
            isFirst = false
        } else {
            path.lineTo(x, y)
        }
    }

    val stroke = Stroke(
        width = style.lineWidth.toPx(),
        cap = StrokeCap.Round,
        join = androidx.compose.ui.graphics.StrokeJoin.Round
    )

    if (style.lineGradient != null) {
        drawPath(path, style.lineGradient, style = stroke)
    } else {
        drawPath(path, style.lineColor, style = stroke)
    }
}

private fun measureMaxLabelWidth(
    min: Double, 
    max: Double, 
    formatter: (Double) -> String, 
    measurer: TextMeasurer, 
    style: TextStyle
): Float {
    val l1 = measurer.measure(formatter(min), style).size.width
    val l2 = measurer.measure(formatter(max), style).size.width
    // Measure a middle value too just in case
    val l3 = measurer.measure(formatter((min + max) / 2), style).size.width
    return maxOf(l1, l2, l3).toFloat()
}
