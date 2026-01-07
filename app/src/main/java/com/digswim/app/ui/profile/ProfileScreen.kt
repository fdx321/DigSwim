package com.digswim.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.digswim.app.ui.theme.NeonGreen

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val garminEmail by viewModel.garminEmail.collectAsState(initial = "")
    val garminPassword by viewModel.garminPassword.collectAsState(initial = "")
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showGarminDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditProfileDialog(
            currentNickname = userProfile.nickname,
            currentBio = userProfile.bio,
            currentAvatarUrl = userProfile.avatarUrl,
            onDismiss = { showEditDialog = false },
            onConfirm = { newNickname, newBio, newAvatarUrl ->
                viewModel.updateProfile(newNickname, newBio, newAvatarUrl)
                showEditDialog = false
            }
        )
    }
    
    if (showGarminDialog) {
        GarminLoginDialog(
            currentEmail = garminEmail ?: "",
            currentPassword = garminPassword ?: "",
            onDismiss = { showGarminDialog = false },
            onConfirm = { email, pass ->
                viewModel.saveGarminCredentials(email, pass)
                showGarminDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // User Info Section
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color(0xFF333333), CircleShape)
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile.avatarUrl != null) {
                        AsyncImage(
                            model = userProfile.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column {
                    Text(
                        text = userProfile.nickname,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userProfile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ID: ${userProfile.nickname}", // Placeholder if we had an ID
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Placeholder for future stats or menu items
            // Garmin Settings Item
            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Color(0xFF333333), thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGarminDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings, // Or a better icon like Watch
                    contentDescription = "Garmin Settings",
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Garmin 账号设置",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    if (!garminEmail.isNullOrEmpty()) {
                        Text(
                            text = "已绑定: $garminEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "未绑定",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarminLoginDialog(
    currentEmail: String,
    currentPassword: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var email by remember { mutableStateOf(currentEmail) }
    var password by remember { mutableStateOf(currentPassword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Garmin 账号", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = NeonGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = NeonGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, password) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("保存", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentNickname: String,
    currentBio: String,
    currentAvatarUrl: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    var bio by remember { mutableStateOf(currentBio) }
    var avatarUrl by remember { mutableStateOf(currentAvatarUrl) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                avatarUrl = uri.toString()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text("编辑个人资料", color = Color.White)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar Edit
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    // Edit Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = NeonGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("个性签名") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = NeonGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nickname, bio, avatarUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("保存", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        }
    )
}
