package com.example.a1234567889.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.a1234567889.R
import com.example.a1234567889.models.*
import com.example.a1234567889.ui.PuzzleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: PuzzleViewModel,
    onNavigateBack: () -> Unit
) {
    val entries by viewModel.leaderboard.collectAsState()
    
    var selectedMode by remember { mutableStateOf(GameMode.NUMBERS) }
    var selectedSize by remember { mutableIntStateOf(4) }
    var sortBy by remember { mutableStateOf(LeaderboardSortBy.TIME) }
    var onlyFriends by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf(LeaderboardPeriod.ALL_TIME) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMode, selectedSize, sortBy, onlyFriends, period, searchQuery) {
        viewModel.refreshLeaderboard(selectedMode, selectedSize, sortBy, onlyFriends, period, searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.leaderboards)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList, "Фильтры")
                    }
                    IconButton(onClick = { 
                        viewModel.refreshLeaderboard(selectedMode, selectedSize, sortBy, onlyFriends, period, searchQuery)
                    }) {
                        Icon(Icons.Default.Refresh, "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showFilters) {
                LeaderboardFiltersPanel(
                    selectedMode = selectedMode,
                    onModeSelected = { selectedMode = it },
                    selectedSize = selectedSize,
                    onSizeSelected = { selectedSize = it },
                    sortBy = sortBy,
                    onSortBySelected = { sortBy = it },
                    onlyFriends = onlyFriends,
                    onOnlyFriendsToggled = { onlyFriends = it },
                    period = period,
                    onPeriodSelected = { period = it }
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Поиск по никнейму...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            LeaderboardHeader()

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(entries) { entry ->
                    LeaderboardRow(
                        entry = entry, 
                        sortBy = sortBy,
                        onPlayerClick = { /* Navigate to Profile */ },
                        onChallengeClick = { /* Send Challenge */ }
                    )
                }
                
                if (entries.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Записей не найдено", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardFiltersPanel(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit,
    selectedSize: Int,
    onSizeSelected: (Int) -> Unit,
    sortBy: LeaderboardSortBy,
    onSortBySelected: (LeaderboardSortBy) -> Unit,
    onlyFriends: Boolean,
    onOnlyFriendsToggled: (Boolean) -> Unit,
    period: LeaderboardPeriod,
    onPeriodSelected: (LeaderboardPeriod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode & Size
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedMode.name, modifier = Modifier.weight(1f), maxLines = 1)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    GameMode.entries.forEach { mode ->
                        DropdownMenuItem(text = { Text(mode.name) }, onClick = { onModeSelected(mode); expanded = false })
                    }
                }
            }
            
            Box(modifier = Modifier.weight(0.5f)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${selectedSize}x${selectedSize}", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (3..8).forEach { size ->
                        DropdownMenuItem(text = { Text("${size}x${size}") }, onClick = { onSizeSelected(size); expanded = false })
                    }
                }
            }
        }

        // Sort & Period
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (sortBy == LeaderboardSortBy.TIME) "По времени" else "По ходам", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    LeaderboardSortBy.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(if (s == LeaderboardSortBy.TIME) "По времени" else "По ходам") }, onClick = { onSortBySelected(s); expanded = false })
                    }
                }
            }
            
            Box(modifier = Modifier.weight(1f)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(when(period) {
                            LeaderboardPeriod.ALL_TIME -> "За все время"
                            LeaderboardPeriod.SEASON -> "За сезон"
                            LeaderboardPeriod.WEEK -> "За неделю"
                        }, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    LeaderboardPeriod.entries.forEach { p ->
                        DropdownMenuItem(text = { Text(when(p) {
                            LeaderboardPeriod.ALL_TIME -> "За все время"
                            LeaderboardPeriod.SEASON -> "За сезон"
                            LeaderboardPeriod.WEEK -> "За неделю"
                        }) }, onClick = { onPeriodSelected(p); expanded = false })
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onOnlyFriendsToggled(!onlyFriends) }) {
            Switch(checked = onlyFriends, onCheckedChange = onOnlyFriendsToggled)
            Spacer(Modifier.width(12.dp))
            Text("Только друзья")
        }
    }
}

@Composable
fun LeaderboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Игрок", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Время", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, fontSize = 12.sp)
        Text("Ходы", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, fontSize = 12.sp)
    }
}

@Composable
fun LeaderboardRow(
    entry: LeaderboardEntry, 
    sortBy: LeaderboardSortBy,
    onPlayerClick: (String) -> Unit,
    onChallengeClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayerClick(entry.userId) },
        color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
                if (entry.rank <= 3) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = when (entry.rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            else -> Color(0xFFCD7F32)
                        }
                    )
                } else {
                    Text(text = entry.rank.toString(), fontSize = 14.sp)
                }
            }

            // Nickname & Avatar
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.nickname,
                        fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (entry.rankChange != 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (entry.rankChange > 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                null,
                                tint = if (entry.rankChange > 0) Color.Green else Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = Math.abs(entry.rankChange).toString(),
                                fontSize = 10.sp,
                                color = if (entry.rankChange > 0) Color.Green else Color.Red
                            )
                        }
                    }
                }
                
                if (!entry.isCurrentUser) {
                    IconButton(onClick = { onChallengeClick(entry.userId) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.FlashOn, "Вызов", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("ВЫ", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Stats
            val hasNoScore = entry.isCurrentUser && entry.timeSeconds == 0L && entry.moves == 0
            if (hasNoScore) {
                Text(
                    text = "—",
                    modifier = Modifier.width(130.dp),
                    textAlign = TextAlign.End,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = formatTime(entry.timeSeconds),
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.End,
                    fontSize = 14.sp,
                    fontWeight = if (sortBy == LeaderboardSortBy.TIME) FontWeight.Bold else FontWeight.Normal,
                    color = if (sortBy == LeaderboardSortBy.TIME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.moves.toString(),
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End,
                    fontSize = 14.sp,
                    fontWeight = if (sortBy == LeaderboardSortBy.MOVES) FontWeight.Bold else FontWeight.Normal,
                    color = if (sortBy == LeaderboardSortBy.MOVES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
