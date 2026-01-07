package com.digswim.app.ui.report

import android.graphics.Picture
import android.graphics.Bitmap
import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.graphics.asAndroidBitmap
import android.util.Log
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.digswim.app.R
import com.digswim.app.model.SwimActivity
import com.digswim.app.ui.components.MonthlyBarChart
import com.digswim.app.ui.home.HomeViewModel
import com.digswim.app.ui.home.formatDuration
import com.digswim.app.ui.home.formatDurationDecimal
import com.digswim.app.ui.home.formatPace
import com.digswim.app.ui.theme.NeonGreen
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.LocalDate

import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt



import com.digswim.app.utils.ShareUtils
import com.digswim.app.model.MonthlySummary

import com.digswim.app.ui.components.ShareBottomSheet
import com.digswim.app.ui.components.SharePlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    yearMonth: YearMonth,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    Log.d("MonthlyReportScreen", "Composing MonthlyReportScreen for $yearMonth")

    LaunchedEffect(yearMonth) {
        Log.d("MonthlyReportScreen", "Selecting month in ViewModel: $yearMonth")
        viewModel.selectMonth(yearMonth)
    }

    val currentMonthlySummary by viewModel.currentMonthlySummary.collectAsState()
    val allActivities by viewModel.weeklyActivities.collectAsState()
    
    // Filter activities
    val monthActivities = remember(allActivities, yearMonth) {
        allActivities.filter { 
            val date = it.startTime.toLocalDate()
            date.year == yearMonth.year && date.month == yearMonth.month
        }
    }

    // Stats for "Red Box"
    val previousMonth = yearMonth.minusMonths(1)
    val previousMonthActivities = remember(allActivities, previousMonth) {
        allActivities.filter { 
            val date = it.startTime.toLocalDate()
            date.year == previousMonth.year && date.month == previousMonth.month
        }
    }
    
    val currentDistance = currentMonthlySummary?.totalDistanceMeters ?: 0
    val prevDistance = previousMonthActivities.sumOf { it.distanceMeters }
    val diffDistance = currentDistance - prevDistance
    
    val firstActivity = allActivities.minByOrNull { it.startTime }
    val lifetimeDistance = allActivities.sumOf { it.distanceMeters }
    val lifetimeStart = firstActivity?.startTime?.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) ?: "2025年01月01日" // fallback
    
    val currentMonthDays = monthActivities.map { it.startTime.toLocalDate() }.distinct().size
    val lifetimeDays = allActivities.map { it.startTime.toLocalDate() }.distinct().size

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Share Sheet State
    var showShareSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "泳者月志",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { showShareSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Gray
                    )
                }
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            SummaryCard(
                yearMonth = yearMonth, 
                totalDistanceMeters = currentDistance
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            DataOverviewCard(
                yearMonth = yearMonth,
                dailyDistances = currentMonthlySummary?.dailyDistances ?: emptyList(),
                totalDuration = currentMonthlySummary?.totalDurationSeconds ?: 0L,
                swimCount = currentMonthlySummary?.swimCount ?: 0,
                totalCalories = currentMonthlySummary?.totalCalories ?: 0,
                avgPace = currentMonthlySummary?.avgPaceSecondsPer100m ?: 0,
                avgHeartRate = currentMonthlySummary?.avgHeartRate ?: 0,
                avgSwolf = currentMonthlySummary?.avgSwolf ?: 0
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            AchievementsCard(
                yearMonth = yearMonth,
                activities = monthActivities,
                currentDistance = currentDistance,
                diffDistance = diffDistance,
                lifetimeStart = lifetimeStart,
                lifetimeDistance = lifetimeDistance,
                currentMonthDays = currentMonthDays,
                lifetimeDays = lifetimeDays
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E),
                dragHandle = null,
                shape = RectangleShape
            ) {
                ShareBottomSheet(
                    onDismiss = { showShareSheet = false },
                    onShareClick = { platform, isLongImage ->
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showShareSheet = false
                            
                            if (platform == SharePlatform.Save) {
                                coroutineScope.launch {
                                    Toast.makeText(context, "正在生成图片...", Toast.LENGTH_SHORT).show()
                                    val content: @Composable () -> Unit = if (isLongImage) {
                                        {
                                            MonthlyReportShareContent(
                                                yearMonth = yearMonth,
                                                currentDistance = currentDistance,
                                                currentMonthlySummary = currentMonthlySummary,
                                                monthActivities = monthActivities,
                                                diffDistance = diffDistance,
                                                lifetimeStart = lifetimeStart,
                                                lifetimeDistance = lifetimeDistance,
                                                currentMonthDays = currentMonthDays,
                                                lifetimeDays = lifetimeDays
                                            )
                                        }
                                    } else {
                                        {
                                            MonthlyReportShortShareContent(
                                                yearMonth = yearMonth,
                                                currentDistance = currentDistance,
                                                currentMonthlySummary = currentMonthlySummary
                                            )
                                        }
                                    }
                                    
                                    val bitmap = ShareUtils.captureBitmapFromComposable(context, content)
                                    bitmap?.let {
                                        val uri = ShareUtils.saveBitmapToGallery(context, it, "DigSwim_Report_${yearMonth}")
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
                                coroutineScope.launch {
                                    Toast.makeText(context, "正在生成图片...", Toast.LENGTH_SHORT).show()
                                    val content: @Composable () -> Unit = if (isLongImage) {
                                        {
                                            MonthlyReportShareContent(
                                                yearMonth = yearMonth,
                                                currentDistance = currentDistance,
                                                currentMonthlySummary = currentMonthlySummary,
                                                monthActivities = monthActivities,
                                                diffDistance = diffDistance,
                                                lifetimeStart = lifetimeStart,
                                                lifetimeDistance = lifetimeDistance,
                                                currentMonthDays = currentMonthDays,
                                                lifetimeDays = lifetimeDays
                                            )
                                        }
                                    } else {
                                        {
                                            MonthlyReportShortShareContent(
                                                yearMonth = yearMonth,
                                                currentDistance = currentDistance,
                                                currentMonthlySummary = currentMonthlySummary
                                            )
                                        }
                                    }
                                    
                                    val bitmap = ShareUtils.captureBitmapFromComposable(context, content)
                                    bitmap?.let {
                                        val uri = ShareUtils.saveBitmapToGallery(context, it, "DigSwim_Report_${yearMonth}")
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
                    contentLong = {
                        MonthlyReportShareContent(
                            yearMonth = yearMonth,
                            currentDistance = currentDistance,
                            currentMonthlySummary = currentMonthlySummary,
                            monthActivities = monthActivities,
                            diffDistance = diffDistance,
                            lifetimeStart = lifetimeStart,
                            lifetimeDistance = lifetimeDistance,
                            currentMonthDays = currentMonthDays,
                            lifetimeDays = lifetimeDays
                        )
                    },
                    contentShort = {
                        MonthlyReportShortShareContent(
                            yearMonth = yearMonth,
                            currentDistance = currentDistance,
                            currentMonthlySummary = currentMonthlySummary
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MonthlyReportShortShareContent(
    yearMonth: YearMonth,
    currentDistance: Int,
    currentMonthlySummary: MonthlySummary?
) {
    Column(
        modifier = Modifier
            .width(375.dp) // Fixed width for consistent share image
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DataOverviewCard(
            yearMonth = yearMonth,
            dailyDistances = currentMonthlySummary?.dailyDistances ?: emptyList(),
            totalDuration = currentMonthlySummary?.totalDurationSeconds ?: 0L,
            swimCount = currentMonthlySummary?.swimCount ?: 0,
            totalCalories = currentMonthlySummary?.totalCalories ?: 0,
            avgPace = currentMonthlySummary?.avgPaceSecondsPer100m ?: 0,
            avgHeartRate = currentMonthlySummary?.avgHeartRate ?: 0,
            avgSwolf = currentMonthlySummary?.avgSwolf ?: 0
        )
        
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
fun MonthlyReportShareContent(
    yearMonth: YearMonth,
    currentDistance: Int,
    currentMonthlySummary: MonthlySummary?,
    monthActivities: List<SwimActivity>,
    diffDistance: Int,
    lifetimeStart: String,
    lifetimeDistance: Int,
    currentMonthDays: Int,
    lifetimeDays: Int
) {
    Column(
        modifier = Modifier
            .width(375.dp) // Fixed width for consistent share image
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            yearMonth = yearMonth, 
            totalDistanceMeters = currentDistance
        )

        DataOverviewCard(
            yearMonth = yearMonth,
            dailyDistances = currentMonthlySummary?.dailyDistances ?: emptyList(),
            totalDuration = currentMonthlySummary?.totalDurationSeconds ?: 0L,
            swimCount = currentMonthlySummary?.swimCount ?: 0,
            totalCalories = currentMonthlySummary?.totalCalories ?: 0,
            avgPace = currentMonthlySummary?.avgPaceSecondsPer100m ?: 0,
            avgHeartRate = currentMonthlySummary?.avgHeartRate ?: 0,
            avgSwolf = currentMonthlySummary?.avgSwolf ?: 0
        )

        AchievementsCard(
            yearMonth = yearMonth,
            activities = monthActivities,
            currentDistance = currentDistance,
            diffDistance = diffDistance,
            lifetimeStart = lifetimeStart,
            lifetimeDistance = lifetimeDistance,
            currentMonthDays = currentMonthDays,
            lifetimeDays = lifetimeDays
        )
        
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
fun SummaryCard(
    yearMonth: YearMonth, 
    totalDistanceMeters: Int,
    modifier: Modifier = Modifier
) {
    Log.d("MonthlyReportScreen", "Rendering SummaryCard")
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f), // Keep the tall aspect ratio
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black), // Pure black background for the pattern
        border = BorderStroke(1.dp, Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Pattern
            SwimmerDotPattern(
                modifier = Modifier.fillMaxSize()
            )

            // Content Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Top Section: Date and Distance
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp)) 
                    
                    Text(
                        text = yearMonth.format(DateTimeFormatter.ofPattern("yyyy.MM")),
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val distanceKm = totalDistanceMeters / 1000f
                    Text(
                        text = String.format("%.1f km", distanceKm),
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                
                // Bottom Section: Logo
                Text(
                    text = "DIGSWIM",
                    color = NeonGreen, 
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun SwimmerDotPattern(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Load the vector drawable as an ImageBitmap to use as a mask reference
    // We can't easily "read" pixels from a vector drawable directly in Canvas without drawing it first.
    // So we'll use a bitmap approach: Draw vector to a small bitmap, then sample it.
    
    // Create a bitmap from the vector drawable once
    val patternBitmap = remember {
        val vectorDrawable = context.getDrawable(R.drawable.ic_swimmer_pattern)
        vectorDrawable?.let {
            // Use a low-res bitmap for the pattern sampling (e.g. 100x100 grid)
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
            bitmap
        }
    }

    Canvas(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val maxDotRadius = 2.5.dp.toPx()
        val minDotRadius = 0.5.dp.toPx()
        val spacing = 8.dp.toPx()
        
        if (size.width <= 0 || size.height <= 0 || patternBitmap == null) return@Canvas
        
        val rows = (size.height / spacing).toInt()
        val cols = (size.width / spacing).toInt()

        val activeColor = NeonGreen
        val inactiveColor = Color(0xFF1A1A1A)

        // Center of the swimmer shape (approximate) for radial gradient effect
        // We assume the vector drawable is roughly centered
        val shapeCenterX = 0.5f * size.width
        val shapeCenterY = 0.5f * size.height

        for (row in 0..rows) {
            for (col in 0..cols) {
                val cx = col * spacing
                val cy = row * spacing
                
                // Map current canvas coordinate to bitmap coordinate (0..99)
                val bx = (cx / size.width * patternBitmap.width).toInt().coerceIn(0, patternBitmap.width - 1)
                val by = (cy / size.height * patternBitmap.height).toInt().coerceIn(0, patternBitmap.height - 1)
                
                // Check alpha of the pixel in the bitmap
                val pixel = patternBitmap.getPixel(bx, by)
                val alpha = android.graphics.Color.alpha(pixel)
                
                if (alpha > 50) { // Threshold: If pixel is visible in the vector shape
                    // Calculate distance from center of shape to create gradient effect
                    val nx = cx / size.width
                    val ny = cy / size.height
                    val distToCenter = dist(nx, ny, 0.5f, 0.5f)
                    val gradientFactor = (1f - distToCenter * 2f).coerceIn(0.3f, 1f)
                    
                    val radius = minDotRadius + (maxDotRadius - minDotRadius) * gradientFactor
                    val dotAlpha = gradientFactor
                    
                    drawCircle(
                        color = activeColor.copy(alpha = dotAlpha), 
                        radius = radius, 
                        center = Offset(cx, cy)
                    )
                } else {
                    drawCircle(color = inactiveColor, radius = minDotRadius, center = Offset(cx, cy))
                }
            }
        }
    }
}

fun isInsideSwimmerShape(x: Float, y: Float): Boolean {
    // DigSwim Logo Style - Minimalist Speed Swimmer
    // Focusing on the "Arrow/Dart" shape look
    
    // 1. Head (More forward, tucked)
    if (dist(x, y, 0.75f, 0.4f) < 0.08f) return true
    
    // 2. Upper Body / Torso (Streamlined main mass)
    if (isInEllipse(x, y, 0.5f, 0.5f, 0.3f, 0.12f, 15f)) return true
    
    // 3. Right Arm (Leading, reaching far forward, almost horizontal)
    // Connecting torso to head and beyond
    if (isInEllipse(x, y, 0.7f, 0.45f, 0.2f, 0.06f, 10f)) return true
    
    // 4. Left Arm (Recovery, high elbow)
    // The iconic triangle shape on top
    if (isInTriangle(x, y, 
        0.4f, 0.45f, // Shoulder
        0.55f, 0.25f, // Elbow (High point)
        0.65f, 0.4f  // Hand entry
    )) return true
    
    // 5. Legs (Kicking, tapered)
    if (isInEllipse(x, y, 0.25f, 0.6f, 0.15f, 0.05f, 25f)) return true

    return false
}

// Helper for triangle shape (common in swimmer logos for the recovery arm)
fun isInTriangle(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Boolean {
    val denominator = ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3))
    val a = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / denominator
    val b = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / denominator
    val c = 1 - a - b
    return a >= 0 && b >= 0 && c >= 0
}

fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
}

fun isInEllipse(x: Float, y: Float, cx: Float, cy: Float, rx: Float, ry: Float, angleDegrees: Float): Boolean {
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)
    
    val dx = x - cx
    val dy = y - cy
    
    // Rotate point backwards to align with unrotated ellipse
    val rotX = dx * cosA + dy * sinA
    val rotY = -dx * sinA + dy * cosA
    
    return (rotX * rotX) / (rx * rx) + (rotY * rotY) / (ry * ry) <= 1.0
}



@Composable
fun DataOverviewCard(
    yearMonth: YearMonth, 
    dailyDistances: List<Int>,
    totalDuration: Long,
    swimCount: Int,
    totalCalories: Int,
    avgPace: Int,
    avgHeartRate: Int = 142, // Added param
    avgSwolf: Int = 69, // Added param
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        border = BorderStroke(1.dp, Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "月度总览",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "DIGSWIM",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            // Chart
            MonthlyBarChart(
                dailyDistances = dailyDistances,
                selectedMonth = yearMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 16.dp)
            )
            
            // Stats Grid (2x2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp, 
                        color = Color(0xFF333333).copy(alpha = 0.5f), // Subtle border
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // Cool Dot Matrix Background
                Canvas(modifier = Modifier.matchParentSize().background(Color.Transparent)) {
                    val dotRadius = 0.8.dp.toPx() // Smaller dots
                    val spacing = 10.dp.toPx()
                    
                    val rows = (size.height / spacing).toInt()
                    val cols = (size.width / spacing).toInt()
                    
                    for (row in 0..rows) {
                        for (col in 0..cols) {
                            val cx = col * spacing
                            val cy = row * spacing
                            
                            // Calculate distance from center for gradient opacity
                            val nx = cx / size.width
                            val ny = cy / size.height
                            val distToCenter = dist(nx, ny, 0.5f, 0.5f)
                            
                            // Gradient: Center is visible, edges fade out
                            // Made the gradient smoother and subtler
                            val alpha = (1f - distToCenter * 2.0f).coerceIn(0f, 1f)
                            
                            if (alpha > 0) {
                                drawCircle(
                                    color = Color.Gray.copy(alpha = alpha * 0.15f), // Reduced opacity for subtlety
                                    radius = dotRadius,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }
                }

                // Stats Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp) // Inner padding
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(label = "游泳时长", value = formatDuration(totalDuration))
                        StatBox(label = "总距离(km)", value = String.format("%.1f", (dailyDistances.sum() / 1000f)))
                        StatBox(label = "平均配速", value = formatPace(avgPace))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(label = "平均心率", value = "$avgHeartRate")
                        StatBox(label = "游泳次数", value = "$swimCount")
                        StatBox(label = "卡路里(kcal)", value = "$totalCalories")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                     Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(label = "平均swolf", value = "$avgSwolf")
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(modifier = Modifier.width(100.dp)) { // Adjusted width slightly to fit 3 in a row comfortably
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun AchievementsCard(
    yearMonth: YearMonth,
    activities: List<SwimActivity>,
    currentDistance: Int,
    diffDistance: Int,
    lifetimeStart: String,
    lifetimeDistance: Int,
    currentMonthDays: Int,
    lifetimeDays: Int,
    modifier: Modifier = Modifier
) {
    val maxDistanceActivity = activities.maxByOrNull { it.distanceMeters }
    val maxDurationActivity = activities.maxByOrNull { it.durationSeconds }
    val fastestPaceActivity =
        activities.minByOrNull { if (it.distanceMeters > 0) it.durationSeconds.toFloat() / it.distanceMeters else Float.MAX_VALUE }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        border = BorderStroke(1.dp, Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "本月成就",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "DIGSWIM",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Red Box Content (Comparison Stats) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // 1. Monthly Distance Comparison
                val diffKm = kotlin.math.abs(diffDistance) / 1000f
                val diffText = if (diffDistance >= 0) "比上月多游 ${
                    String.format(
                        "%.1f",
                        diffKm
                    )
                }km" else "比上月少游 ${String.format("%.1f", diffKm)}km"
                ComparisonRow(
                    icon = Icons.Default.Pool,
                    highlightText = "本月游量 ${String.format("%.1f", currentDistance / 1000f)}km",
                    normalText = diffText
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Lifetime Distance
                val currentYearStart = LocalDate.of(yearMonth.year, 1, 1)
                val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                val lifetimeStartStr = currentYearStart.format(formatter)
                
                ComparisonRow(
                    icon = Icons.Default.EmojiEvents,
                    highlightText = "${lifetimeStartStr} 至今",
                    normalText = "游泳距离突破 ${String.format("%.1f", lifetimeDistance / 1000f)}km"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Days
                ComparisonRow(
                    icon = Icons.Default.Today,
                    highlightText = "本月游泳 $currentMonthDays 天",
                    normalText = "生涯累计游泳 $lifetimeDays 天"
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            DashedDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.Gray.copy(alpha = 0.3f)
            )

            // --- Records ---

            // 1. Max Distance
            AchievementRow(
                label = "最远距离",
                value = if (maxDistanceActivity != null) {
                    if (maxDistanceActivity.distanceMeters >= 1000)
                        String.format("%.2f km", maxDistanceActivity.distanceMeters / 1000f)
                    else
                        "${maxDistanceActivity.distanceMeters} m"
                } else "-",
                date = maxDistanceActivity?.startTime?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    ?: "",
                color = NeonGreen,
                iconResId = R.drawable.ic_distance_road
            )

            DashedDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.Gray.copy(alpha = 0.2f)
            )

            // 2. Max Duration
            AchievementRow(
                label = "最长时间",
                value = if (maxDurationActivity != null) formatDuration(maxDurationActivity.durationSeconds) else "-",
                date = maxDurationActivity?.startTime?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    ?: "",
                color = Color(0xFF00BFFF), // Blueish
                iconResId = R.drawable.ic_duration_clock
            )

            DashedDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.Gray.copy(alpha = 0.2f)
            )

            // 3. Fastest Pace
            val pace = if (fastestPaceActivity != null && fastestPaceActivity.distanceMeters > 0) {
                formatPace((fastestPaceActivity.durationSeconds * 100 / fastestPaceActivity.distanceMeters).toInt())
            } else "-"

            AchievementRow(
                label = "最快配速",
                value = pace,
                date = fastestPaceActivity?.startTime?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    ?: "",
                color = Color(0xFFFFA500), // Orange
                iconResId = R.drawable.ic_pace_bolt
            )
            Spacer(modifier = Modifier.height(8.dp)) // Small bottom padding to avoid hugging bottom edge too tightly but much less than before
        }
    }
}

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray,
    thickness: androidx.compose.ui.unit.Dp = 1.dp,
    dashLength: androidx.compose.ui.unit.Dp = 4.dp,
    gapLength: androidx.compose.ui.unit.Dp = 4.dp
) {
    Canvas(modifier = modifier.fillMaxWidth().height(thickness)) {
        val pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()), 0f
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = pathEffect
        )
    }
}

@Composable
fun ComparisonRow(icon: ImageVector, highlightText: String, normalText: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = highlightText,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = normalText,
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}

@Composable
fun AchievementRow(label: String, value: String, date: String, color: Color, iconResId: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.Gray, fontSize = 14.sp)
            Text(text = date, color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

