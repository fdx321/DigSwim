package com.digswim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.vectorResource
import com.digswim.app.R

enum class SharePlatform(val label: String, val icon: ImageVector?, val iconRes: Int?, val color: Color) {
    XiaoHongShu("小红书", null, R.drawable.ic_xiaohongshu_logo, Color(0xFFFF2442)),
    WeChat("微信", Icons.Default.Chat, null, Color(0xFF07C160)),
    WeChatMoments("朋友圈", Icons.Default.Groups, null, Color(0xFF07C160)),
    Save("保存", Icons.Default.Download, null, Color.White) // Special case
}

@Composable
fun ShareBottomSheet(
    onDismiss: () -> Unit,
    onShareClick: (SharePlatform, Boolean) -> Unit, // Platform, isLongImage
    contentLong: @Composable () -> Unit,
    contentShort: @Composable () -> Unit
) {
    var isLongImage by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .systemBarsPadding()
    ) {
        // 1. Top Bar (Close Button + Tabs)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray
                )
            }

            // Tabs (Centered)
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.Center
            ) {
                TabButton(
                    text = "短图",
                    isSelected = !isLongImage,
                    onClick = { isLongImage = false }
                )
                Spacer(modifier = Modifier.width(16.dp))
                TabButton(
                    text = "长图",
                    isSelected = isLongImage,
                    onClick = { isLongImage = true }
                )
            }
        }

        // 2. Preview Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Scrollable container for the preview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // We render the content here directly. 
                // Scaling might be needed if the content is fixed width (375.dp) and screen is smaller,
                // but usually scrolling is fine for preview.
                if (isLongImage) {
                    contentLong()
                } else {
                    contentShort()
                }
            }
        }

        // 3. Bottom Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Order: Save, Moments, WeChat, Xiaohongshu
                ShareActionButton(SharePlatform.Save) { onShareClick(it, isLongImage) }
                ShareActionButton(SharePlatform.WeChatMoments) { onShareClick(it, isLongImage) }
                ShareActionButton(SharePlatform.WeChat) { onShareClick(it, isLongImage) }
                ShareActionButton(SharePlatform.XiaoHongShu) { onShareClick(it, isLongImage) }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF333333) else Color.Transparent,
        contentColor = if (isSelected) Color.White else Color.Gray,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ShareActionButton(
    platform: SharePlatform,
    onClick: (SharePlatform) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick(platform) }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2C)),
            contentAlignment = Alignment.Center
        ) {
            if (platform == SharePlatform.XiaoHongShu) {
                Text(
                    text = "小红书",
                    color = platform.color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                val iconVector = if (platform.icon != null) {
                    platform.icon
                } else if (platform.iconRes != null) {
                    ImageVector.vectorResource(id = platform.iconRes)
                } else {
                    Icons.Default.Star
                }

                Icon(
                    imageVector = iconVector,
                    contentDescription = platform.label,
                    tint = platform.color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = platform.label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
