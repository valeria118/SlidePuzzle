package com.example.a1234567889.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1234567889.ui.PuzzleViewModel
import com.example.a1234567889.ui.components.MyFriendQrDialog
import com.example.a1234567889.ui.components.ScanFriendQrButton

/**
 * Lab item 13 — "Чтение QR-кодов".
 *
 * Dedicated "Мои друзья" screen: shows the friend list, lets the user search by nickname,
 * and offers two QR-based ways to add a friend — show your own code, or scan a friend's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: PuzzleViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val friends = remember(currentUser?.friends) { viewModel.getFriends() }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showMyQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        searchResults = if (searchQuery.length >= 2) viewModel.searchUsers(searchQuery) else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои друзья") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    ScanFriendQrButton(viewModel)
                    IconButton(onClick = { showMyQrDialog = true }) {
                        Icon(Icons.Default.QrCode2, contentDescription = "Показать мой QR-код")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Добавить по QR-коду",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Покажите свой код другу или отсканируйте его код, чтобы мгновенно добавить друг друга.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showMyQrDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Мой код")
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Сканировать", color = MaterialTheme.colorScheme.onPrimary)
                                }
                                ScanFriendQrButton(
                                    viewModel,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    showIcon = false
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Поиск игрока по никнейму:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Введите никнейм (мин. 2 символа)") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        if (searchResults.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            searchResults.forEach { (userId, nick) ->
                                val isFriend = friends.any { it.first == userId }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(nick, modifier = Modifier.weight(1f))
                                    if (isFriend) {
                                        Text(
                                            "друг",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                viewModel.addFriend(userId)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "$nick добавлен в друзья",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PersonAdd,
                                                "Добавить в друзья",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (searchQuery.length >= 2) {
                            Spacer(Modifier.height(8.dp))
                            Text("Игроков не найдено", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }

            item {
                Text(
                    "Ваши друзья (${friends.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (friends.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PersonAdd,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Друзей пока нет", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Найдите по нику или добавьте по QR-коду",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                items(friends) { (userId, nick) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(nick, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        IconButton(
                            onClick = {
                                viewModel.removeFriend(userId)
                                android.widget.Toast.makeText(
                                    context,
                                    "$nick удалён из друзей",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, "Удалить из друзей", modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showMyQrDialog) {
        MyFriendQrDialog(viewModel, onDismiss = { showMyQrDialog = false })
    }
}
