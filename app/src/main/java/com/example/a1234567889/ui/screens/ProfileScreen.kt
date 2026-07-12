package com.example.a1234567889.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import com.example.a1234567889.R
import com.example.a1234567889.models.*

// ─────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    stats: UserStats,
    achievements: List<Achievement>,
    onLogout: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToCardGallery: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateBack: () -> Unit,
    onSaveProfile: (nickname: String, email: String) -> Unit = { _, _ -> },
    onChangePassword: (current: String, new: String) -> Unit = { _, _ -> },
    profileError: String? = null,
    profileSuccess: Boolean = false,
    onClearProfileStatus: () -> Unit = {},
    onAvatarChanged: (String) -> Unit = {},
    onLaunchPhotoPicker: () -> Unit = {},
    onLaunchCamera: () -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAvatarMenu by remember { mutableStateOf(false) }
    var showProfileOptionsMenu by remember { mutableStateOf(false) }
    val profileContext = androidx.compose.ui.platform.LocalContext.current

    // Lab item 17 — camera capture for the avatar photo.
    val takePhotoLauncher = com.example.a1234567889.logic.rememberCameraPhotoLauncher { uri ->
        onAvatarChanged(uri.toString())
    }
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onLaunchCamera()
            takePhotoLauncher()
        } else {
            com.example.a1234567889.logic.ToastHelper.show(profileContext, "Камера недоступна без разрешения")
        }
    }

    // Preview items calculation
    val previewItems = remember(user) {
        val images = user?.gallery?.filter { it.isCollected } ?: emptyList()
        val cards = user?.cardCollection ?: emptyList()
        (images.map { it as Any } + cards.map { it as Any })
            .sortedByDescending { 
                when(it) {
                    is ImageGalleryItem -> it.dateCollected ?: 0L
                    is CollectedCard -> it.obtainedAt
                    else -> 0L
                }
            }
    }

    // Show snackbar on success/error
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(profileSuccess) {
        if (profileSuccess) {
            snackbarHostState.showSnackbar("Профиль успешно обновлён")
            onClearProfileStatus()
        }
    }
    LaunchedEffect(profileError) {
        if (profileError != null) {
            snackbarHostState.showSnackbar(profileError)
            onClearProfileStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (user != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать профиль")
                        }
                        Box {
                            IconButton(onClick = { showProfileOptionsMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Меню профиля")
                            }
                            DropdownMenu(expanded = showProfileOptionsMenu, onDismissRequest = { showProfileOptionsMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Сменить фото") },
                                    leadingIcon = { Icon(Icons.Default.CameraAlt, null) },
                                    onClick = {
                                        showProfileOptionsMenu = false
                                        showAvatarMenu = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Поделиться профилем") },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = {
                                        showProfileOptionsMenu = false
                                        com.example.a1234567889.logic.IntentHelper.shareText(
                                            profileContext,
                                            "Я играю в Slide Puzzle под именем ${user.nickname}! Присоединяйся 🧩"
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Выйти") },
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, null) },
                                    onClick = {
                                        showProfileOptionsMenu = false
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Avatar
                Box(modifier = Modifier.size(100.dp)) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user?.avatarUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(user.avatarUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (user != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { showAvatarMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Изменить фото",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user?.nickname ?: stringResource(R.string.guest),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (user?.email != null) {
                    Text(
                        text = user.email,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Row 1: Collection + Achievements
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        GalleryButton(
                            count = user?.gallery?.count { it.isCollected } ?: 0,
                            total = user?.gallery?.size ?: 0,
                            onClick = onNavigateToGallery
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AchievementsButton(
                            count = achievements.count { it.isUnlocked },
                            total = achievements.size,
                            hasNew = achievements.any { it.isNew },
                            onClick = onNavigateToAchievements
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Stats
            item {
                StatsSection(stats, user?.winStreak ?: 0)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Gallery preview (images + cards)
            if (previewItems.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.gallery), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Смотреть все",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToGallery() },
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(previewItems) { item ->
                            when(item) {
                                is ImageGalleryItem -> GalleryThumbnail(item)
                                is CollectedCard -> CardPreviewThumbnail(item, onClick = onNavigateToCardGallery)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Account actions
            if (user != null) {
                item {
                    OutlinedButton(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Изменить пароль", maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.logout), maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Dialogs
    if (showEditDialog && user != null) {
        EditProfileDialog(
            currentNickname = user.nickname,
            currentEmail = user.email ?: "",
            onSave = { nick, email ->
                onSaveProfile(nick, email)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onSave = { current, new ->
                onChangePassword(current, new)
                showPasswordDialog = false
            },
            onDismiss = { showPasswordDialog = false }
        )
    }

    // Lab item 17 — choose between camera capture and the gallery for the avatar photo.
    if (showAvatarMenu) {
        AlertDialog(
            onDismissRequest = { showAvatarMenu = false },
            title = { Text("Изменить фото профиля") },
            text = { Text("Выберите источник изображения") },
            confirmButton = {
                TextButton(onClick = {
                    showAvatarMenu = false
                    if (com.example.a1234567889.logic.CameraUtil.hasCameraPermission(profileContext)) {
                        onLaunchCamera()
                        takePhotoLauncher()
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Камера")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAvatarMenu = false
                    onLaunchPhotoPicker()
                }) {
                    Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея")
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Edit Profile Dialog
// ─────────────────────────────────────────────────────────────

@Composable
fun EditProfileDialog(
    currentNickname: String,
    currentEmail: String,
    onSave: (nickname: String, email: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    var email by remember { mutableStateOf(currentEmail) }
    var nicknameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }

    val nicknameError = if (nicknameTouched && nickname.trim().length < 2)
        "Никнейм должен содержать не менее 2 символов" else null
    val emailError = if (emailTouched && !isValidEmailLocal(email.trim()))
        "Введите корректный email (должен содержать @)" else null

    val canSave = nickname.trim().length >= 2 && isValidEmailLocal(email.trim())

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Редактировать профиль", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it; nicknameTouched = true },
                    label = { Text(stringResource(R.string.nickname)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nicknameError != null,
                    supportingText = nicknameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailTouched = true },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, null) }
                )
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена", maxLines = 1, softWrap = false) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            nicknameTouched = true
                            emailTouched = true
                            if (canSave) onSave(nickname.trim(), email.trim())
                        }
                    ) { Text("Сохранить", maxLines = 1, softWrap = false) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Change Password Dialog
// ─────────────────────────────────────────────────────────────

@Composable
fun ChangePasswordDialog(
    onSave: (currentPassword: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }

    val newPasswordError = if (touched && newPassword.length < 6)
        "Минимум 6 символов" else null
    val confirmError = if (touched && confirmPassword != newPassword)
        "Пароли не совпадают" else null
    val currentError = if (touched && currentPassword.isBlank())
        "Введите текущий пароль" else null

    val canSave = currentPassword.isNotBlank() && newPassword.length >= 6 && newPassword == confirmPassword

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Изменить пароль", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                // Current password
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Текущий пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = currentError != null,
                    supportingText = currentError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { showCurrent = !showCurrent }) {
                            Icon(
                                if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))

                // New password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Новый пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = newPasswordError != null,
                    supportingText = newPasswordError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    } ?: { Text("Минимум 6 символов", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(
                                if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))

                // Confirm password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Подтвердите пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = confirmError != null,
                    supportingText = confirmError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                    trailingIcon = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            Icon(
                                if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена", maxLines = 1, softWrap = false) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            touched = true
                            if (canSave) onSave(currentPassword, newPassword)
                        }
                    ) { Text("Изменить", maxLines = 1, softWrap = false) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helper
// ─────────────────────────────────────────────────────────────

private fun isValidEmailLocal(email: String): Boolean =
    email.contains('@') && email.contains('.') && email.indexOf('@') < email.lastIndexOf('.')

// ─────────────────────────────────────────────────────────────
// Sub-composables (unchanged from before)
// ─────────────────────────────────────────────────────────────

@Composable
fun GalleryButton(count: Int, total: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() }.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Image, null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.gallery), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            Text("$count / $total", fontSize = 12.sp)
        }
    }
}

@Composable
fun CardPreviewThumbnail(card: CollectedCard, onClick: () -> Unit) {
    val context = LocalContext.current
    val rankColor = when (card.rank) {
        CardRank.BRONZE -> Color(0xFFCD7F32)
        CardRank.SILVER -> Color(0xFFC0C0C0)
        CardRank.GOLD -> Color(0xFFFFD700)
    }

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .border(1.dp, rankColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Image(
            painter = rememberAsyncImagePainter(com.example.a1234567889.logic.PictureProvider.resUri(card.photoRes)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Frame overlay
        val frameResName = com.example.a1234567889.logic.CardRewardLogic.frameRes(card.rank)
        val frameResId = context.resources.getIdentifier(frameResName, "drawable", context.packageName)
        if (frameResId != 0) {
            Image(
                painter = painterResource(id = frameResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(rankColor.copy(alpha = 0.7f))
                .padding(2.dp)
        ) {
            Text(
                text = "CARD",
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun AchievementsButton(count: Int, total: Int, hasNew: Boolean, onClick: () -> Unit) {
    BadgedBox(badge = { if (hasNew) Badge { Text("!") } }) {
        Card(
            modifier = Modifier.clickable { onClick() }.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.achievements), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text("$count / $total", fontSize = 12.sp)
            }
        }
    }
}


@Composable
fun StatsSection(stats: UserStats, winStreak: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.statistics), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.total_games))
                Text("${stats.totalGames}", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.total_time))
                Text(formatTime(stats.totalTime), fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.win_streak))
                Text("$winStreak", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GalleryThumbnail(item: ImageGalleryItem) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.uri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (item.bestTime != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(2.dp)
            ) {
                Text(
                    text = formatTime(item.bestTime),
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    val isHidden = achievement.isHidden && !achievement.isUnlocked
    ListItem(
        headlineContent = { Text(if (isHidden) "???" else achievement.title) },
        supportingContent = {
            Column {
                Text(if (isHidden) "Тссс... это секрет!" else achievement.description)
                if (!achievement.isUnlocked && achievement.maxProgress > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progress.toFloat() / achievement.maxProgress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            if (achievement.isUnlocked) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = when (achievement.rarity) {
                        AchievementRarity.GOLD -> Color(0xFFFFD700)
                        AchievementRarity.SILVER -> Color(0xFFC0C0C0)
                        AchievementRarity.BRONZE -> Color(0xFFCD7F32)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    )
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(
        user = User("1", "Николай", email = "email@test.ru"),
        stats = UserStats(10, 3600),
        achievements = listOf(
            Achievement("1", "Первый шаг", "Сделайте свой первый ход", AchievementCategory.GENERAL, isUnlocked = true),
            Achievement("2", "Скрытый", "???", AchievementCategory.GENERAL, isHidden = true)
        ),
        onLogout = {},
        onNavigateToGallery = {},
        onNavigateToCardGallery = {},
        onNavigateToAchievements = {},
        onNavigateBack = {}
    )
}
