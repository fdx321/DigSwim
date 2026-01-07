package com.digswim.app.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import kotlinx.coroutines.launch
import com.digswim.app.model.MetricPoint
import com.digswim.app.model.SwimActivityDetail
import com.digswim.app.ui.components.AxisConfig
import com.digswim.app.ui.components.AxisPosition
import com.digswim.app.ui.components.ChartStyle
import com.digswim.app.ui.components.DigSwimLineChart
import com.digswim.app.ui.theme.NeonGreen
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.digswim.app.utils.ShareUtils
import com.digswim.app.ui.components.ShareBottomSheet
import com.digswim.app.ui.components.SharePlatform
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.drawToBitmap
import com.digswim.app.ui.theme.DigSwimTheme

import com.digswim.app.model.SwimActivity
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: String,
    onBackClick: () -> Unit,
    viewModel: ActivityDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(activityId) {
        viewModel.loadActivityDetail(activityId)
    }

    val uiState by viewModel.activityDetail.collectAsState()
    var showShareSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                activityId = activityId,
                uiState = uiState,
                onBackClick = onBackClick,
                onShareClick = { showShareSheet = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = NeonGreen
                    )
                }
                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    DetailContent(state.data)
                }
            }
        }

        if (showShareSheet) {
            val state = uiState
            if (state is UiState.Success) {
                ModalBottomSheet(
                    onDismissRequest = { showShareSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF1E1E1E),
                    dragHandle = null, // We have our own layout
                    shape = RectangleShape
                ) {
                    ShareBottomSheet(
                        onDismiss = { showShareSheet = false },
                        onShareClick = { platform, isLongImage ->
                            // Close sheet first? Or keep open? User experience varies.
                            // Usually close after action.
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showShareSheet = false
                                
                                if (platform == SharePlatform.Save) {
                                    scope.launch {
                                        Toast.makeText(context, "正在生成图片...", Toast.LENGTH_SHORT).show()
                                        val data = state.data
                                        val content: @Composable () -> Unit = if (isLongImage) {
                                            { ActivityDetailShareContent(data) }
                                        } else {
                                            { ActivityDetailShortShareContent(data) }
                                        }
                                        
                                        val bitmap = ShareUtils.captureBitmapFromComposable(context, content)
                                        bitmap?.let {
                                            val uri = ShareUtils.saveBitmapToGallery(context, it, "DigSwim_Activity_${activityId}")
                                            if (uri != null) {
                                                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                            }
                                        } ?: run {
                                            Toast.makeText(context, "生成图片失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else if (platform == SharePlatform.WeChatMoments || platform == SharePlatform.WeChat || platform == SharePlatform.XiaoHongShu) {
                                    scope.launch {
                                        Toast.makeText(context, "正在生成图片...", Toast.LENGTH_SHORT).show()
                                        val data = state.data
                                        val content: @Composable () -> Unit = if (isLongImage) {
                                            { ActivityDetailShareContent(data) }
                                        } else {
                                            { ActivityDetailShortShareContent(data) }
                                        }
                                        
                                        val bitmap = ShareUtils.captureBitmapFromComposable(context, content)
                                        bitmap?.let {
                                            val uri = ShareUtils.saveBitmapToGallery(context, it, "DigSwim_Activity_${activityId}")
                                            if (uri != null) {
                                                when (platform) {
                                                    SharePlatform.WeChat -> ShareUtils.shareImage(context, uri, "com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI")
                                                    SharePlatform.WeChatMoments -> ShareUtils.shareImage(context, uri, "com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI")
                                                    SharePlatform.XiaoHongShu -> ShareUtils.shareImage(context, uri, "com.xingin.xhs")
                                                    else -> ShareUtils.shareImage(context, uri)
                                                }
                                            } else {
                                                Toast.makeText(context, "分享失败：保存图片出错", Toast.LENGTH_SHORT).show()
                                            }
                                        } ?: run {
                                            Toast.makeText(context, "生成图片失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "分享到 ${platform.label} (开发中)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contentLong = { ActivityDetailShareContent(state.data) },
                        contentShort = { ActivityDetailShortShareContent(state.data) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopAppBar(
    activityId: String,
    uiState: UiState<SwimActivityDetail>,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 8.dp)) {
            val title = if (uiState is UiState.Success) {
                uiState.data.summary.activityName ?: "游泳详情"
            } else {
                "游泳详情"
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (uiState is UiState.Success) {
                val date = uiState.data.summary.startTime
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        IconButton(onClick = onShareClick) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
        }
    }
}

@Composable
fun ShareBottomSheetContent(onOptionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "分享到",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 16.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ShareOptionItem(
                icon = Icons.Default.Download,
                label = "保存到相册",
                color = Color.White,
                onClick = { onOptionClick("保存到相册") }
            )
            ShareOptionItem(
                icon = Icons.Default.Chat,
                label = "微信好友",
                color = Color(0xFF07C160),
                onClick = { onOptionClick("微信好友") }
            )
            ShareOptionItem(
                icon = Icons.Default.Groups,
                label = "朋友圈",
                color = Color(0xFF07C160),
                onClick = { onOptionClick("朋友圈") }
            )
            ShareOptionItem(
                icon = Icons.Default.Star,
                label = "小红书",
                color = Color(0xFFFF2442),
                onClick = { onOptionClick("小红书") }
            )
        }
    }
}

@Composable
fun ShareOptionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF2C2C2C), shape = MaterialTheme.shapes.medium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CoolDistanceHeader(distanceMeters: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.Black)
    ) {
        // 1. Cool Background
        CoolParticleBackground()
        
        // 2. Text Content
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$distanceMeters",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = NeonGreen
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "m",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }
    }
}

@Composable
fun CoolParticleBackground() {
    val particles = remember {
        List(40) {
            Particle(
                x = Math.random().toFloat(),
                y = Math.random().toFloat(),
                radius = (Math.random() * 3 + 1).toFloat(),
                alpha = (Math.random() * 0.5 + 0.1).toFloat(),
                vx = (Math.random() * 0.004 - 0.002).toFloat(),
                vy = (Math.random() * 0.004 - 0.002).toFloat()
            )
        }
    }

    var trigger by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                trigger = it
                particles.forEach { p ->
                    p.x += p.vx
                    p.y += p.vy

                    // Wrap around
                    if (p.x < 0) p.x += 1f
                    if (p.x > 1) p.x -= 1f
                    if (p.y < 0) p.y += 1f
                    if (p.y > 1) p.y -= 1f
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val _t = trigger // Trigger redraw
        val width = size.width
        val height = size.height

        // Draw connections
        for (i in particles.indices) {
            for (j in i + 1 until particles.size) {
                val p1 = particles[i]
                val p2 = particles[j]
                val dx = (p1.x - p2.x) * width
                val dy = (p1.y - p2.y) * height
                val distSq = dx * dx + dy * dy
                val maxDist = 60.dp.toPx()
                val maxDistSq = maxDist * maxDist

                if (distSq < maxDistSq) {
                    val alpha = (1f - distSq / maxDistSq) * 0.2f
                    drawLine(
                        color = NeonGreen.copy(alpha = alpha),
                        start = Offset(p1.x * width, p1.y * height),
                        end = Offset(p2.x * width, p2.y * height),
                        strokeWidth = 1f
                    )
                }
            }
        }

        // Draw particles
        particles.forEach { p ->
            drawCircle(
                color = NeonGreen.copy(alpha = p.alpha),
                radius = p.radius.dp.toPx(),
                center = Offset(p.x * width, p.y * height)
            )
        }
    }
}

data class Particle(
    var x: Float,
    var y: Float,
    val radius: Float,
    val alpha: Float,
    val vx: Float,
    val vy: Float
)

@Composable
private fun DetailContent(detail: SwimActivityDetail) {
    val summary = detail.summary
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. Big Distance with Cool Background
        CoolDistanceHeader(summary.distanceMeters)

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Grid Stats (Divider removed above)
        StatsGrid(summary)

        Spacer(modifier = Modifier.height(32.dp))
        Divider(color = Color.DarkGray, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(32.dp))

        // 3. Charts
        // We don't need absolute startTime anymore for relative X axis
        
        if (detail.metrics.pacePoints.isNotEmpty()) {
            ChartSection(
                title = "配速统计", 
                points = detail.metrics.pacePoints, 
                isPace = true, 
                color = Color(0xFFFF9800), 
                icon = Icons.Default.Speed
            )
            Spacer(modifier = Modifier.height(32.dp))
            Divider(color = Color.DarkGray, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        if (detail.metrics.hrPoints.isNotEmpty()) {
            ChartSection(
                title = "心率统计", 
                points = detail.metrics.hrPoints, 
                isPace = false, 
                color = Color(0xFFF44336), 
                icon = Icons.Default.Favorite
            )
            Spacer(modifier = Modifier.height(32.dp))
            Divider(color = Color.DarkGray, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        if (detail.metrics.swolfPoints.isNotEmpty()) {
            ChartSection(
                title = "Swolf", 
                points = detail.metrics.swolfPoints, 
                isPace = false, 
                color = Color(0xFF2196F3), 
                icon = Icons.Default.DirectionsRun // Use DirectionsRun as a proxy for efficiency/strokes
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ActivityDetailShareContent(detail: SwimActivityDetail) {
    val summary = detail.summary
    
    Column(
        modifier = Modifier
            .width(375.dp) // Fixed width for consistent share image
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Title / Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = summary.activityName ?: "游泳详情",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = summary.startTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "DIGSWIM",
                color = NeonGreen,
                fontWeight = FontWeight.Bold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Big Distance with Cool Background
        CoolDistanceHeader(summary.distanceMeters)

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Grid Stats
        StatsGrid(summary)

        Spacer(modifier = Modifier.height(32.dp))
        Divider(color = Color.DarkGray, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(32.dp))

        // 3. Charts
        if (detail.metrics.pacePoints.isNotEmpty()) {
            ChartSection(
                title = "配速统计", 
                points = detail.metrics.pacePoints, 
                isPace = true, 
                color = Color(0xFFFF9800), 
                icon = Icons.Default.Speed
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (detail.metrics.hrPoints.isNotEmpty()) {
            ChartSection(
                title = "心率统计", 
                points = detail.metrics.hrPoints, 
                isPace = false, 
                color = Color(0xFFF44336), 
                icon = Icons.Default.Favorite
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (detail.metrics.swolfPoints.isNotEmpty()) {
            ChartSection(
                title = "Swolf", 
                points = detail.metrics.swolfPoints, 
                isPace = false, 
                color = Color(0xFF2196F3), 
                icon = Icons.Default.DirectionsRun 
            )
        }
        
        // Footer
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), 
            contentAlignment = Alignment.Center
        ) {
             Text(
                 text = "Generated by DigSwim", 
                 color = Color.Gray, 
                 fontSize = 12.sp,
                 fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
             )
        }
    }
}

@Composable
fun ActivityDetailShortShareContent(detail: SwimActivityDetail) {
    val summary = detail.summary
    
    Column(
        modifier = Modifier
            .width(375.dp) // Fixed width for consistent share image
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Title / Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = summary.activityName ?: "游泳详情",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = summary.startTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "DIGSWIM",
                color = NeonGreen,
                fontWeight = FontWeight.Bold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Big Distance with Cool Background
        CoolDistanceHeader(summary.distanceMeters)

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Grid Stats
        StatsGrid(summary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Footer
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), 
            contentAlignment = Alignment.Center
        ) {
             Text(
                 text = "Generated by DigSwim", 
                 color = Color.Gray, 
                 fontSize = 12.sp,
                 fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
             )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ChartSection(
    title: String, 
    points: List<MetricPoint>, 
    isPace: Boolean, 
    color: Color, 
    icon: ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Simple Min/Max Legend
        val validPoints = points.filter { it.yValue > 0 }
        if (validPoints.isNotEmpty()) {
            val avg = validPoints.map { it.yValue }.average()
            val max = validPoints.maxOf { it.yValue }
            val min = validPoints.minOf { it.yValue }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if(isPace) formatPace(avg.toInt()) else avg.toInt().toString(),
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    text = if(isPace) formatPace(min.toInt()) else max.toInt().toString(), 
                    color = Color.Gray, fontSize = 24.sp 
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("平均", color = Color.Gray, fontSize = 12.sp)
                Text(if(isPace) "最快" else "最大", color = Color.Gray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val axisConfig = AxisConfig(
            showXAxis = true,
            showYAxis = true,
            showGrid = true,
            invertYAxis = isPace, 
            yAxisPosition = AxisPosition.Right,
            tickCountY = 5,
            tickCountX = 5,
            yAxisFormatter = { value ->
                if (isPace) formatPace(value.toInt()) else value.toInt().toString()
            },
            xAxisFormatter = { seconds ->
                formatDuration(seconds) // Format as MM:SS relative time
            }
        )
        
        val style = ChartStyle(
            lineColor = color,
            lineGradient = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.5f), color)
            ),
            gridColor = Color.Gray.copy(alpha = 0.2f),
            axisTextColor = Color.Gray,
            axisTextSize = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = Color.Gray)
        )

        DigSwimLineChart(
            dataPoints = points,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            axisConfig = axisConfig,
            style = style
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

// Helpers
fun formatDuration(seconds: Long): String {
    return formatDuration(seconds.toDouble())
}

fun formatDuration(seconds: Double): String {
    val s = seconds.toLong()
    val m = s / 60
    val sec = s % 60
    return String.format("%02d:%02d", m, sec)
}

fun formatPace(secondsPer100m: Int): String {
    val m = secondsPer100m / 60
    val s = secondsPer100m % 60
    return String.format("%d'%02d\"", m, s)
}

@Composable
fun StatsGrid(summary: SwimActivity) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        DotMatrixBackground(Modifier.matchParentSize())
        
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("时长", formatDuration(summary.durationSeconds))
                StatItem("平均配速", formatPace(summary.avgPaceSecondsPer100m))
                StatItem("平均心率", summary.avgHeartRate?.toString() ?: "--")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("卡路里", "${summary.calories}")
                StatItem("总划水数", summary.totalStrokes?.toString() ?: "--")
                StatItem("Swolf", summary.swolf?.toString() ?: "--")
            }
        }
    }
}

@Composable
fun DotMatrixBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val dotRadius = 0.8.dp.toPx()
        val spacing = 10.dp.toPx()
        
        val rows = (size.height / spacing).toInt()
        val cols = (size.width / spacing).toInt()
        
        for (row in 0..rows) {
            for (col in 0..cols) {
                val cx = col * spacing
                val cy = row * spacing
                
                val nx = cx / size.width
                val ny = cy / size.height
                val distToCenter = sqrt((nx - 0.5f) * (nx - 0.5f) + (ny - 0.5f) * (ny - 0.5f))
                
                val alpha = (1f - distToCenter * 2.0f).coerceIn(0f, 1f)
                
                if (alpha > 0) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = alpha * 0.3f), // Increased opacity from 0.15f to 0.3f
                        radius = dotRadius,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

