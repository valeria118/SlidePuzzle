package com.example.a1234567889.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1234567889.models.*
import com.example.a1234567889.ui.PuzzleViewModel
import kotlinx.coroutines.delay

@Composable
fun TournamentsTab(viewModel: PuzzleViewModel) {
    val tournaments by viewModel.tournaments.collectAsState(initial = emptyList())
    val currentUser = viewModel.currentUser.collectAsState(initial = null).value
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTournamentName by remember { mutableStateOf("") }

    val myTournaments = tournaments.filter { t -> currentUser != null && t.participants.contains(currentUser.id) }
    val openTournaments = tournaments.filter { t -> !t.isStarted && !t.isFinished && (currentUser == null || !t.participants.contains(currentUser.id)) }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Создать турнир") },
            text = {
                OutlinedTextField(
                    value = newTournamentName,
                    onValueChange = { newTournamentName = it },
                    label = { Text("Название турнира") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTournamentName.isNotBlank()) {
                            viewModel.createTournament(newTournamentName, GameSettings(mode = GameMode.COMPETITION))
                            newTournamentName = ""
                            showCreateDialog = false
                        }
                    },
                    enabled = newTournamentName.isNotBlank()
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Отмена") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Турниры", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (currentUser != null) {
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Создать")
                    }
                }
            }
        }

        if (myTournaments.isNotEmpty()) {
            item {
                Text("Мои турниры", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(myTournaments) { tournament ->
                TournamentCard(tournament = tournament, isJoined = true, onJoin = {})
            }
        }

        if (openTournaments.isNotEmpty()) {
            item {
                Text("Открытые турниры", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(openTournaments.filter { t -> myTournaments.none { m -> m.id == t.id } }) { tournament ->
                TournamentCard(
                    tournament = tournament,
                    isJoined = false,
                    onJoin = { viewModel.joinTournament(tournament.id) }
                )
            }
        }

        if (openTournaments.isEmpty() && myTournaments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.4f))
                        Spacer(Modifier.height(16.dp))
                        Text("Турниров пока нет", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        if (currentUser != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("Создайте первый турнир!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentCard(tournament: Tournament, isJoined: Boolean, onJoin: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD700))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tournament.name, fontWeight = FontWeight.Bold)
                Text(
                    "${tournament.participants.size} участников",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            if (!isJoined) {
                TextButton(onClick = onJoin) {
                    Text("Вступить")
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Участвую",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionSelectionScreen(
    user: User?,
    viewModel: PuzzleViewModel,
    onStartMatch: (GameSettings) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Соревнование", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                val tabs = listOf("Дуэль", "Рейтинг", "Команды", "ИИ", "Турниры")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1, softWrap = false) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> DuelTab(viewModel, onStartMatch)
                    1 -> RankedTab(user?.rankedStats ?: RankedStats(), onStartMatch)
                    2 -> TeamsTab(viewModel, onStartMatch)
                    3 -> AiDuelTab(viewModel, user, onStartMatch)
                    4 -> TournamentsTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun DuelTab(viewModel: PuzzleViewModel, onStartMatch: (GameSettings) -> Unit) {
    var showWaitingRoom by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showMyQrDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    // Recomputed whenever the user's friend list changes, so newly added friends show up immediately
    val friends = remember(currentUser?.friends) { viewModel.getFriends() }

    LaunchedEffect(searchQuery) {
        searchResults = if (searchQuery.length >= 2) viewModel.searchUsers(searchQuery) else emptyList()
    }

    if (showWaitingRoom && viewModel.selectedOpponent != null) {
        WaitingRoom(
            opponentNickname = viewModel.selectedOpponent!!.second,
            settings = viewModel.competitionSettings,
            onSettingsChanged = { viewModel.competitionSettings = it },
            onStart = {
                onStartMatch(
                    viewModel.competitionSettings.copy(
                        competitionType = CompetitionType.DUEL_FRIEND,
                        isAiMatch = false
                    )
                )
            },
            onCancel = { showWaitingRoom = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Сыграть с другом", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Добавить друга по QR-коду",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { showMyQrDialog = true }) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Мой код")
                    }
                    com.example.a1234567889.ui.components.ScanFriendQrButton(viewModel)
                }
            }

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
                                    .clickable {
                                        viewModel.selectedOpponent = userId to nick
                                        showWaitingRoom = true
                                        searchQuery = ""
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(nick, modifier = Modifier.weight(1f))
                                if (isFriend) {
                                    Text("друг", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    IconButton(
                                        onClick = {
                                            viewModel.addFriend(userId)
                                            android.widget.Toast.makeText(context, "$nick добавлен в друзья", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, "Добавить в друзья", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
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

            if (friends.isNotEmpty()) {
                Text("Ваши друзья:", style = MaterialTheme.typography.titleSmall)
                friends.forEach { (userId, nick) ->
                    FriendItem(
                        nickname = nick,
                        isOnline = true, // in a real app this would come from presence
                        onClick = {
                            viewModel.selectedOpponent = userId to nick
                            showWaitingRoom = true
                        },
                        onRemove = {
                            viewModel.removeFriend(userId)
                            android.widget.Toast.makeText(context, "$nick удалён из друзей", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("Друзей пока нет", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        Text("Найдите игроков выше и добавьте их", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showMyQrDialog) {
        com.example.a1234567889.ui.components.MyFriendQrDialog(viewModel, onDismiss = { showMyQrDialog = false })
    }
}

@Composable
fun AiDuelTab(viewModel: PuzzleViewModel, user: User?, onStartMatch: (GameSettings) -> Unit) {
    val aiPersonality = remember(viewModel.competitionSettings.aiDifficulty) {
        when (viewModel.competitionSettings.aiDifficulty) {
            ChallengeDifficulty.EASY -> listOf(
                "Робот-новичок «Винтик». Делает много ошибок, идеально для тренировки.",
                "ИИ «Пиксель Джуниор». Только учится собирать пазлы — не торопится."
            )
            ChallengeDifficulty.MEDIUM -> listOf(
                "Опытный ИИ «Логик». Хорошо играет, но иногда ошибается.",
                "ИИ «Тактик». Уверенно собирает пазл, но не без помарок."
            )
            ChallengeDifficulty.HARD -> listOf(
                "Сильный ИИ «Молния». Редко ошибается, решает головоломки быстро.",
                "ИИ «Стратег». Просчитывает ходы наперёд — будьте начеку."
            )
            ChallengeDifficulty.EXPERT -> listOf(
                "Мастер-ИИ «Сингулярность». Почти не ошибается. Победить будет крайне сложно!",
                "ИИ «Гроссмейстер». Решает пазлы практически безошибочно."
            )
        }.random()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Дуэль с ИИ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // AI Personality card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.SmartToy,
                    null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    aiPersonality,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        SizeSelector(selectedSize = viewModel.competitionSettings.size, onSizeSelected = { viewModel.competitionSettings = viewModel.competitionSettings.copy(size = it) })

        Text("Сложность ИИ:", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ChallengeDifficulty.entries.forEach { diff ->
                FilterChip(
                    selected = viewModel.competitionSettings.aiDifficulty == diff,
                    onClick = { viewModel.competitionSettings = viewModel.competitionSettings.copy(aiDifficulty = diff) },
                    label = {
                        Text(
                            text = when(diff) {
                                ChallengeDifficulty.EASY -> "Лёгкий"
                                ChallengeDifficulty.MEDIUM -> "Средний"
                                ChallengeDifficulty.HARD -> "Сложный"
                                ChallengeDifficulty.EXPERT -> "Эксперт"
                            },
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("Тип плиток:", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(GameMode.NUMBERS, GameMode.COLORS, GameMode.IMAGE).forEach { m ->
                val isImageUnavailable = m == GameMode.IMAGE && viewModel.competitionSettings.imageUri == null
                FilterChip(
                    selected = viewModel.competitionSettings.competitionVisualMode == m,
                    enabled = !isImageUnavailable,
                    onClick = {
                        viewModel.competitionSettings = viewModel.competitionSettings.copy(
                            competitionVisualMode = m,
                            paletteType = if (m == GameMode.COLORS) ColorPaletteType.RAINBOW else viewModel.competitionSettings.paletteType
                        )
                    },
                    label = { Text(when(m) {
                        GameMode.NUMBERS -> "Цифры"
                        GameMode.COLORS -> "Цвета"
                        GameMode.IMAGE -> "Картинка"
                        else -> ""
                    }) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (viewModel.competitionSettings.competitionVisualMode == GameMode.IMAGE && viewModel.competitionSettings.imageUri == null) {
            Text(
                "Чтобы играть на картинке, выберите её в галерее в меню профиля.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onStartMatch(viewModel.competitionSettings.copy(isAiMatch = true, competitionType = CompetitionType.DUEL_AI)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("НАЧАТЬ ИГРУ", fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun WaitingRoom(
    opponentNickname: String,
    settings: GameSettings,
    onSettingsChanged: (GameSettings) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Комната ожидания", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, null) }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar("Вы", true)
            Text("VS", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
            PlayerAvatar(opponentNickname, false)
        }

        Spacer(Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Настройки матча", fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Размер:")
                    Row {
                        (3..5).forEach { s ->
                            FilterChip(
                                selected = settings.size == s,
                                onClick = { onSettingsChanged(settings.copy(size = s)) },
                                label = { Text("${s}x${s}") },
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Победа по:")
                    Row {
                        FilterChip(
                            selected = settings.winnerCriteria == "TIME",
                            onClick = { onSettingsChanged(settings.copy(winnerCriteria = "TIME")) },
                            label = { Text("Времени") },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        FilterChip(
                            selected = settings.winnerCriteria == "MOVES",
                            onClick = { onSettingsChanged(settings.copy(winnerCriteria = "MOVES")) },
                            label = { Text("Ходам") },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("ГОТОВ", fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun PlayerAvatar(name: String, isReady: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
            if (isReady) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.align(Alignment.BottomEnd).background(Color.White, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(name, fontWeight = FontWeight.Bold)
        Text(if(isReady) "Готов" else "Ожидание...", style = MaterialTheme.typography.labelSmall, color = if(isReady) Color(0xFF4CAF50) else Color.Gray)
    }
}

@Composable
fun RankedTab(stats: RankedStats, onStartMatch: (GameSettings) -> Unit) {
    var isSearching by remember { mutableStateOf(false) }
    var searchTime by remember { mutableIntStateOf(0) }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            while (isSearching) {
                delay(1000)
                searchTime++
                if (searchTime > 3) {
                    isSearching = false
                    onStartMatch(GameSettings(mode = GameMode.COMPETITION, competitionType = CompetitionType.RANKED, isPublicMatch = true, isAiMatch = true))
                }
            }
        } else {
            searchTime = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isSearching) {
            SearchMatchUI(searchTime) { isSearching = false }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = getLeagueColor(stats.league)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "${getLeagueName(stats.league)} ${stats.division}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("MMR: ${stats.mmr}", style = MaterialTheme.typography.bodyLarge)

                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (stats.mmr % 100) / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBoxSmall("Победы", stats.seasonWins.toString())
                StatBoxSmall("Поражения", stats.seasonLosses.toString())
                StatBoxSmall("Win Rate", "${if(stats.seasonWins+stats.seasonLosses > 0) (stats.seasonWins.toFloat() / (stats.seasonWins + stats.seasonLosses) * 100).toInt() else 0}%")
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { isSearching = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("НАЙТИ МАТЧ", fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            Text(
                "Сезон 1 заканчивается через 45 дней",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SearchMatchUI(time: Int, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Поиск достойного соперника...", style = MaterialTheme.typography.titleMedium)
        Text("Прошло времени: ${time}с", color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onCancel) {
            Text("Отмена", maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun TeamsTab(viewModel: PuzzleViewModel, onStartMatch: (GameSettings) -> Unit) {
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val friends = remember(currentUser?.friends) { viewModel.getFriends() }
    var inTeam by remember { mutableStateOf(viewModel.teamName.isNotEmpty()) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var showQrDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Lab item 13 — scan a teammate's QR code to auto-fill the join code.
    val qrScanLauncher = com.example.a1234567889.logic.rememberQrScanLauncher { scanned ->
        if (!scanned.isNullOrBlank()) {
            joinCodeInput = scanned.filter { it.isDigit() }.ifEmpty { scanned }
            com.example.a1234567889.logic.ToastHelper.show(context, "Код считан: $scanned")
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Название команды") },
            text = {
                OutlinedTextField(
                    value = viewModel.teamName,
                    onValueChange = { viewModel.teamName = it },
                    label = { Text("Введите название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.teamName.isNotBlank()) {
                            showNameDialog = false
                            inTeam = true
                        }
                    },
                    enabled = viewModel.teamName.isNotBlank()
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Вступить по коду") },
            text = {
                Column {
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { joinCodeInput = it },
                        label = { Text("Введите код команды") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { qrScanLauncher.launch(com.example.a1234567889.logic.QrUtils.defaultScanOptions()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Сканировать QR-код")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinCodeInput.isNotBlank()) {
                            viewModel.teamName = "Команда #$joinCodeInput"
                            viewModel.teamPartner = "Капитан Команды"
                            inTeam = true
                            showJoinDialog = false
                        }
                    },
                    enabled = joinCodeInput.isNotBlank()
                ) { Text("Вступить") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Пригласить друга") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    if (friends.isEmpty()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("У вас пока нет друзей", color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Найдите игроков и добавьте их во вкладке «Дуэль»",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        friends.forEach { (_, nick) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.teamPartner = nick
                                        showInviteDialog = false
                                        android.widget.Toast.makeText(context, "Приглашение отправлено $nick", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null)
                                Spacer(Modifier.width(12.dp))
                                Text(nick)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Закрыть") }
            }
        )
    }

    if (!inTeam) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Groups, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text("Командные бои 2х2", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Соберитесь с другом и сразитесь против другой команды!", textAlign = TextAlign.Center, color = Color.Gray)

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { showNameDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Создать команду", maxLines = 1, softWrap = false)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showJoinDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Вступить по коду", maxLines = 1, softWrap = false)
            }
        }
    } else {
        TeamLobby(
            teamName = viewModel.teamName,
            captainNickname = currentUser?.nickname ?: "Вы",
            partnerNickname = viewModel.teamPartner,
            onStart = { onStartMatch(GameSettings(mode = GameMode.COMPETITION, competitionType = CompetitionType.TEAM_2V2, isAiMatch = true)) },
            onLeave = { 
                inTeam = false
                viewModel.teamName = ""
                viewModel.teamPartner = null
            },
            onInvite = { showInviteDialog = true }
        )
    }
}

@Composable
fun TeamLobby(
    teamName: String, 
    captainNickname: String, 
    partnerNickname: String?, 
    onStart: () -> Unit, 
    onLeave: () -> Unit,
    onInvite: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showQrCode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Команда: $teamName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Код: ${teamName.hashCode().toString().takeLast(6)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Row {
                IconButton(onClick = {
                    val code = teamName.hashCode().toString().takeLast(6)
                    val inviteLink = "https://slidepuzzle.app/join/$code"
                    val sendIntent: android.content.Intent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "Присоединяйся к моей команде '$teamName' в Slide Puzzle!\nКод: $code\nСсылка: $inviteLink"
                        )
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Пригласить напарника")
                    context.startActivity(shareIntent)
                }) { Icon(Icons.Default.Share, "Share Code") }
                IconButton(onClick = { showQrCode = true }) { Icon(Icons.Default.QrCode, "Показать QR-код") }
                IconButton(onClick = onLeave) { Icon(Icons.AutoMirrored.Filled.Logout, null) }
            }
        }

        if (showQrCode) {
            val code = teamName.hashCode().toString().takeLast(6)
            val inviteLink = "https://slidepuzzle.app/join/$code"
            val qrBitmap = remember(inviteLink) { com.example.a1234567889.logic.QrUtils.generateQrBitmap(inviteLink) }
            AlertDialog(
                onDismissRequest = { showQrCode = false },
                title = { Text("QR-код команды") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR-код приглашения",
                            modifier = Modifier.size(220.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Код: $code", fontWeight = FontWeight.Bold)
                        Text("Партнёр может отсканировать этот код вместо ввода вручную", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQrCode = false }) { Text("Закрыть") }
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        Text("Ваша команда:", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PlayerAvatar("$captainNickname (Вы)", true)
            
            if (partnerNickname != null) {
                PlayerAvatar(partnerNickname, true)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onInvite() }
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Пригласить", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text("Противники:", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PlayerAvatar("Поиск...", false)
            PlayerAvatar("Поиск...", false)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStart,
            enabled = partnerNickname != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (partnerNickname == null) "НУЖЕН НАПАРНИК" else "НАЧАТЬ ПОИСК БИТВЫ", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun FriendItem(nickname: String, isOnline: Boolean, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { if(isOnline) onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
                    .border(2.dp, Color.White, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(nickname, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (isOnline) {
            Text("Играть", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        } else {
            Text("Оффлайн", color = Color.Gray, fontSize = 12.sp)
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Удалить из друзей", modifier = Modifier.size(16.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
private fun StatBoxSmall(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

fun getLeagueColor(league: CompetitionLeague): Color {
    return when(league) {
        CompetitionLeague.BRONZE -> Color(0xFFCD7F32)
        CompetitionLeague.SILVER -> Color(0xFFC0C0C0)
        CompetitionLeague.GOLD -> Color(0xFFFFD700)
        CompetitionLeague.PLATINA -> Color(0xFFE5E4E2)
        CompetitionLeague.DIAMOND -> Color(0xFFB9F2FF)
        CompetitionLeague.MASTER -> Color(0xFF9C27B0)
        CompetitionLeague.GRANDMASTER -> Color(0xFFFF5722)
    }
}

fun getLeagueName(league: CompetitionLeague): String {
    return when(league) {
        CompetitionLeague.BRONZE -> "Бронза"
        CompetitionLeague.SILVER -> "Серебро"
        CompetitionLeague.GOLD -> "Золото"
        CompetitionLeague.PLATINA -> "Платина"
        CompetitionLeague.DIAMOND -> "Алмаз"
        CompetitionLeague.MASTER -> "Мастер"
        CompetitionLeague.GRANDMASTER -> "Грандмастер"
    }
}
