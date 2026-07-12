package com.example.a1234567889.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1234567889.logic.GameEngine
import com.example.a1234567889.models.*

object ChallengeProvider {
    val challenges = listOf(
        // Easy
        ChallengeConfig(ChallengeMode.REVERSE, "Обратный порядок", "Числа располагаются в порядке убывания: от N²–1 до 1.", ChallengeDifficulty.EASY, 0, Icons.Default.SettingsBackupRestore),
        ChallengeConfig(ChallengeMode.MIRROR_H, "Зеркало (Г)", "Каждая строка перевёрнута справа налево. Строки остаются сверху вниз.", ChallengeDifficulty.EASY, 0, Icons.Default.Compare),
        ChallengeConfig(ChallengeMode.MIRROR_V, "Зеркало (В)", "Строки перевёрнуты сверху вниз. Внутри строк порядок сохраняется.", ChallengeDifficulty.EASY, 0, Icons.Default.Flip),
        
        // Medium
        ChallengeConfig(ChallengeMode.SPIRAL_IN, "Спираль (от угла)", "Числа располагаются по спирали по часовой стрелке от угла.", ChallengeDifficulty.MEDIUM, 5, Icons.Default.DataUsage),
        ChallengeConfig(ChallengeMode.MODULO, "Градиент", "Числа возрастают с плавным переходом по строкам.", ChallengeDifficulty.MEDIUM, 12, Icons.Default.Gradient),
        
        // Hard
        ChallengeConfig(ChallengeMode.SPIRAL_OUT, "Спираль (от центра)", "Числа располагаются по спирали, начиная с центра поля.", ChallengeDifficulty.HARD, 15, Icons.Default.FilterCenterFocus),
        ChallengeConfig(ChallengeMode.DIAGONAL, "Диагональ", "Числа группируются по диагоналям главной диагонали.", ChallengeDifficulty.HARD, 20, Icons.Default.CallMade),
        ChallengeConfig(ChallengeMode.TRANSPOSE, "Транспонирование", "Строки и столбцы меняются местами.", ChallengeDifficulty.HARD, 25, Icons.Default.SwapCalls),
        
        // Expert
        ChallengeConfig(ChallengeMode.MAGIC_SQUARE, "Магический квадрат", "Суммы в каждой строке, столбце и диагонали равны.", ChallengeDifficulty.EXPERT, 30, Icons.Default.AutoFixHigh),
        ChallengeConfig(ChallengeMode.SNAKE, "Змея", "Числа располагаются «змейкой» по строкам.", ChallengeDifficulty.EXPERT, 40, Icons.Default.Timeline),
        ChallengeConfig(ChallengeMode.RANDOM, "Случайный", "Генерируется случайная перестановка чисел.", ChallengeDifficulty.EXPERT, 50, Icons.Default.Casino)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeSelectionScreen(
    userStats: UserStats,
    onChallengeSelected: (ChallengeConfig, Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedDifficulty by remember { mutableStateOf<ChallengeDifficulty?>(null) }
    var showStartDialogFor by remember { mutableStateOf<ChallengeConfig?>(null) }
    
    // Calculate completions by difficulty
    val completedIds = userStats.completedChallengeIds
    val easySolved = ChallengeProvider.challenges.count { it.difficulty == ChallengeDifficulty.EASY && completedIds.contains(it.type.name) }
    val mediumSolved = ChallengeProvider.challenges.count { it.difficulty == ChallengeDifficulty.MEDIUM && completedIds.contains(it.type.name) }
    val hardSolved = ChallengeProvider.challenges.count { it.difficulty == ChallengeDifficulty.HARD && completedIds.contains(it.type.name) }
    val solvedCount = userStats.completedChallengesCount
    
    val filteredChallenges = if (selectedDifficulty == null) {
        ChallengeProvider.challenges
    } else {
        ChallengeProvider.challenges.filter { it.difficulty == selectedDifficulty }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Вызовы", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Выполнено: $solvedCount / ${ChallengeProvider.challenges.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val available = ChallengeProvider.challenges.filter { 
                            isChallengeUnlocked(it, easySolved, mediumSolved, hardSolved)
                        }
                        if (available.isNotEmpty()) showStartDialogFor = available.random()
                    }) {
                        Icon(Icons.Default.Casino, "Случайный")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Difficulty Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DifficultyFilterChip(
                    label = "Все",
                    isSelected = selectedDifficulty == null,
                    onClick = { selectedDifficulty = null }
                )
                ChallengeDifficulty.entries.forEach { difficulty ->
                    DifficultyFilterChip(
                        label = when(difficulty) {
                            ChallengeDifficulty.EASY -> "Легкие"
                            ChallengeDifficulty.MEDIUM -> "Средние"
                            ChallengeDifficulty.HARD -> "Сложные"
                            ChallengeDifficulty.EXPERT -> "Эксперт"
                        },
                        isSelected = selectedDifficulty == difficulty,
                        onClick = { selectedDifficulty = difficulty }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredChallenges) { config ->
                    ChallengeCard(
                        config = config,
                        isUnlocked = isChallengeUnlocked(config, easySolved, mediumSolved, hardSolved),
                        easySolved = easySolved,
                        mediumSolved = mediumSolved,
                        hardSolved = hardSolved,
                        onClick = { showStartDialogFor = config }
                    )
                }
            }
        }

        showStartDialogFor?.let { config ->
            ChallengeStartDialog(
                config = config,
                onDismiss = { showStartDialogFor = null },
                onStart = { size ->
                    onChallengeSelected(config, size)
                    showStartDialogFor = null
                }
            )
        }
    }
}

@Composable
fun ChallengeStartDialog(
    config: ChallengeConfig,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit
) {
    var size by remember { mutableIntStateOf(4) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(config.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(config.goalText, style = MaterialTheme.typography.bodyMedium)
                
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    ChallengeMiniPreview(config.type)
                }

                if (config.type != ChallengeMode.MAGIC_SQUARE) {
                    Text("Выберите размер поля:", style = MaterialTheme.typography.labelLarge)
                    SizeSelectorChallenge(
                        selectedSize = size,
                        onSizeSelected = { size = it }
                    )
                } else {
                    Text("Для магического квадрата размер фиксирован: 3×3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    size = 3
                }
            }
        },
        confirmButton = {
            Button(onClick = { onStart(size) }) {
                Text("ИГРАТЬ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ОТМЕНА")
            }
        }
    )
}

@Composable
private fun SizeSelectorChallenge(selectedSize: Int, onSizeSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (3..6).forEach { s ->
            FilterChip(
                selected = selectedSize == s,
                onClick = { onSizeSelected(s) },
                label = { Text("${s}x${s}", maxLines = 1, softWrap = false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DifficultyFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
fun ChallengeCard(
    config: ChallengeConfig,
    isUnlocked: Boolean,
    easySolved: Int,
    mediumSolved: Int,
    hardSolved: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Preview or Lock
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(if (!isUnlocked) Modifier.background(Color.Black.copy(alpha = 0.1f)) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    ChallengeMiniPreview(config.type)
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = config.icon ?: Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = config.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                }
                
                val requirementText = when(config.difficulty) {
                    ChallengeDifficulty.MEDIUM -> "Нужно $easySolved / ${config.requiredSolved} легких"
                    ChallengeDifficulty.HARD -> "Нужно $mediumSolved / ${config.requiredSolved} средних"
                    ChallengeDifficulty.EXPERT -> "Нужно $hardSolved / ${config.requiredSolved} сложных"
                    else -> ""
                }

                Text(
                    text = if (isUnlocked) config.goalText else requirementText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                DifficultyBadge(config.difficulty, isUnlocked)
            }

            if (isUnlocked) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Играть",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun isChallengeUnlocked(config: ChallengeConfig, easySolved: Int, mediumSolved: Int, hardSolved: Int): Boolean {
    return when (config.difficulty) {
        ChallengeDifficulty.EASY -> true
        ChallengeDifficulty.MEDIUM -> easySolved >= config.requiredSolved
        ChallengeDifficulty.HARD -> mediumSolved >= config.requiredSolved
        ChallengeDifficulty.EXPERT -> hardSolved >= config.requiredSolved
    }
}

@Composable
private fun DifficultyBadge(difficulty: ChallengeDifficulty, isUnlocked: Boolean) {
    val color = when (difficulty) {
        ChallengeDifficulty.EASY -> Color(0xFF4CAF50)
        ChallengeDifficulty.MEDIUM -> Color(0xFFFF9800)
        ChallengeDifficulty.HARD -> Color(0xFFF44336)
        ChallengeDifficulty.EXPERT -> Color(0xFF9C27B0)
    }
    
    Surface(
        color = if (isUnlocked) color.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = when(difficulty) {
                ChallengeDifficulty.EASY -> "ЛЕГКИЙ"
                ChallengeDifficulty.MEDIUM -> "СРЕДНИЙ"
                ChallengeDifficulty.HARD -> "СЛОЖНЫЙ"
                ChallengeDifficulty.EXPERT -> "ЭКСПЕРТ"
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) color else Color.Gray
        )
    }
}

@Composable
private fun ChallengeMiniPreview(type: ChallengeMode) {
    val size = 3
    val board = GameEngine.createTargetBoard(size, type, GameMode.NUMBERS)
    
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (r in 0 until size) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until size) {
                    val tile = board.tiles[r][c]
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (tile.isEmpty) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!tile.isEmpty) {
                            Text(
                                tile.number.toString(),
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
