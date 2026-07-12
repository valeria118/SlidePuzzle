package com.example.a1234567889.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.a1234567889.logic.QrUtils
import com.example.a1234567889.logic.rememberQrScanLauncher
import com.example.a1234567889.models.AddFriendResult
import com.example.a1234567889.ui.PuzzleViewModel
import kotlinx.coroutines.launch

/**
 * Lab item 13 — "Чтение QR-кодов".
 *
 * Two reusable pieces, meant to be dropped onto any screen that manages friends:
 *  - [MyFriendQrDialog]: shows the current user's own QR code so a friend can scan it
 *    and instantly add them.
 *  - [ScanFriendQrButton]: an icon button that opens the camera-based QR scanner and,
 *    on a successful scan, calls [PuzzleViewModel.addFriendFromQrPayload] and shows the
 *    resulting outcome (added / already a friend / user not found / invalid code) as a
 *    small result dialog.
 */

@Composable
fun MyFriendQrDialog(viewModel: PuzzleViewModel, onDismiss: () -> Unit) {
    val payload = remember { viewModel.myFriendQrPayload() }
    val nickname = viewModel.currentUser.collectAsState(initial = null).value?.nickname

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
        title = { Text("Мой QR-код") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (payload == null) {
                    Text(
                        "Войдите в аккаунт, чтобы получить свой QR-код.",
                        textAlign = TextAlign.Center
                    )
                } else {
                    val bitmap = remember(payload) { QrUtils.generateQrBitmap(payload) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR-код друга",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    if (!nickname.isNullOrBlank()) {
                        Text(nickname, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Пусть друг отсканирует этот код в разделе «Друзья», чтобы добавить вас.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

/**
 * Icon button that launches the camera QR scanner and adds the scanned user as a friend.
 * Drop this next to any "add friend" UI (search bar, friends list toolbar, etc).
 */
@Composable
fun ScanFriendQrButton(viewModel: PuzzleViewModel, modifier: Modifier = Modifier, showIcon: Boolean = true) {
    val scope = rememberCoroutineScope()
    var resultDialog by remember { mutableStateOf<AddFriendResult?>(null) }
    var showInvalidQrDialog by remember { mutableStateOf(false) }

    val scanLauncher = rememberQrScanLauncher { scanned ->
        if (scanned.isNullOrBlank()) return@rememberQrScanLauncher
        scope.launch {
            val result = viewModel.addFriendFromQrPayload(scanned)
            if (result == AddFriendResult.InvalidCode) {
                showInvalidQrDialog = true
            } else {
                resultDialog = result
            }
        }
    }

    IconButton(
        onClick = { scanLauncher.launch(QrUtils.defaultScanOptions()) },
        modifier = modifier
    ) {
        if (showIcon) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Сканировать QR-код друга")
        }
    }

    resultDialog?.let { result ->
        AddFriendResultDialog(result, onDismiss = { resultDialog = null })
    }

    if (showInvalidQrDialog) {
        AlertDialog(
            onDismissRequest = { showInvalidQrDialog = false },
            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Это не QR-код друга") },
            text = { Text("Отсканированный код не похож на код приглашения этого приложения.") },
            confirmButton = {
                TextButton(onClick = { showInvalidQrDialog = false }) { Text("Понятно") }
            }
        )
    }
}

@Composable
private fun AddFriendResultDialog(result: AddFriendResult, onDismiss: () -> Unit) {
    val (icon, tint, title, message) = when (result) {
        is AddFriendResult.Added -> QrResultPresentation(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50),
            "Друг добавлен!",
            "${result.nickname} теперь в вашем списке друзей."
        )
        is AddFriendResult.AlreadyFriend -> QrResultPresentation(
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primary,
            "Уже друзья",
            "${result.nickname} уже есть в вашем списке друзей."
        )
        AddFriendResult.UserNotFound -> QrResultPresentation(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            "Пользователь не найден",
            "Этот пользователь ещё не входил в приложение на этом устройстве."
        )
        AddFriendResult.IsSelf -> QrResultPresentation(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            "Это ваш собственный код",
            "Нельзя добавить себя в друзья."
        )
        AddFriendResult.NotLoggedIn -> QrResultPresentation(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            "Вы не вошли в аккаунт",
            "Войдите в аккаунт, чтобы добавлять друзей."
        )
        AddFriendResult.InvalidCode -> QrResultPresentation(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            "Неверный код",
            "Не удалось распознать код приглашения."
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null, tint = tint) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ОК") }
        }
    )
}

private data class QrResultPresentation(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val title: String,
    val message: String
)
