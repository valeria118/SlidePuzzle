package com.example.a1234567889.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1234567889.R
import com.example.a1234567889.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: List<Achievement>,
    onResetProgress: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categories = AchievementCategory.values()
    val categoryNames = listOf(
        "Общие", "Цифры", "Картинки", "Цвета", "Вызов", "Соревнование", "На время", "Бесконечный"
    )

    val totalCount = achievements.size
    val unlockedCount = achievements.count { it.isUnlocked }
    val progressPercent = if (totalCount > 0) (unlockedCount.toFloat() / totalCount.toFloat() * 100).toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Достижения") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onResetProgress) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сбросить прогресс")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Summary Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$unlockedCount / $totalCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Прогресс: $progressPercent%",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    CircularProgressIndicator(
                        progress = { if (totalCount > 0) unlockedCount.toFloat() / totalCount.toFloat() else 0f },
                        modifier = Modifier.size(50.dp),
                        strokeWidth = 6.dp,
                    )
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                categories.forEachIndexed { index, _ ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                text = categoryNames.getOrElse(index) { "???" },
                                maxLines = 1,
                                softWrap = false
                            ) 
                        }
                    )
                }
            }

            val filteredAchievements = achievements
                .filter { it.category == categories[selectedTabIndex] }
                .sortedWith(compareByDescending<Achievement> { it.isUnlocked }.thenBy { it.id })

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAchievements) { achievement ->
                    AchievementCard(achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement) {
    val isHidden = achievement.isHidden && !achievement.isUnlocked

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(if (achievement.isUnlocked) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (achievement.isUnlocked)
                            getCategoryColor(achievement.category)
                        else Color.Gray,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isHidden) Icons.Default.Lock else Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHidden) "???" else achievement.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (achievement.isUnlocked)
                        MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = if (isHidden) "Это достижение скрыто. Выполните его, чтобы узнать подробности." else achievement.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!achievement.isUnlocked && achievement.maxProgress > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progress.toFloat() / achievement.maxProgress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Gray.copy(alpha = 0.2f)),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            if (achievement.isUnlocked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = getRarityColor(achievement.rarity),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

fun getCategoryColor(category: AchievementCategory): Color = when (category) {
    AchievementCategory.GENERAL -> Color(0xFF607D8B)
    AchievementCategory.CLASSIC -> Color(0xFF2196F3)
    AchievementCategory.IMAGE -> Color(0xFF4CAF50)
    AchievementCategory.COLORS -> Color(0xFFFF9800)
    AchievementCategory.CHALLENGE -> Color(0xFFFF5722)
    AchievementCategory.COMPETITION -> Color(0xFF9C27B0)
    AchievementCategory.TIME_ATTACK -> Color(0xFFF44336)
    AchievementCategory.ENDLESS -> Color(0xFF795548)
}

fun getRarityColor(rarity: AchievementRarity): Color = when (rarity) {
    AchievementRarity.COMMON -> Color(0xFFB0BEC5)
    AchievementRarity.BRONZE -> Color(0xFFCD7F32)
    AchievementRarity.SILVER -> Color(0xFFC0C0C0)
    AchievementRarity.GOLD -> Color(0xFFFFD700)
    AchievementRarity.PLATINUM -> Color(0xFFE5E4E2)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AchievementsScreenPreview() {
    val mockAchievements = listOf(
        Achievement("1", "Добро пожаловать!", "Открыть игру в первый раз.", AchievementCategory.GENERAL, isUnlocked = true),
        Achievement("2", "Первая победа", "Собрать любую головоломку.", AchievementCategory.GENERAL),
        Achievement("3", "Новичок", "Собрать 10 партий.", AchievementCategory.GENERAL, progress = 5, maxProgress = 10),
        Achievement("4", "Скрытое", "Это описание должно быть скрыто", AchievementCategory.GENERAL, isHidden = true)
    )
    MaterialTheme {
        AchievementsScreen(
            achievements = mockAchievements,
            onResetProgress = {},
            onNavigateBack = {}
        )
    }
}
