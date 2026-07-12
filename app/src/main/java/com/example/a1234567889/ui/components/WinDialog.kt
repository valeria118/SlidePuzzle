package com.example.a1234567889.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.a1234567889.models.*
import kotlinx.coroutines.delay

@Composable
fun WinDialog(
    gameState: GameState,
    onPlayAgain: () -> Unit,
    onNewGame: () -> Unit,
    onMainMenu: () -> Unit,
    onShowStats: () -> Unit,
    onNextLevel: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var cardRevealed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    // Flip animation for the card
    val flipProgress by animateFloatAsState(
        targetValue = if (cardRevealed) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "flip"
    )
    val scaleX = if (flipProgress < 0.5f) (1f - flipProgress * 2f) else ((flipProgress - 0.5f) * 2f)
    val showFront = flipProgress >= 0.5f

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        if (gameState.newCard != null) {
            delay(800)
            cardRevealed = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Festive Header
                    Text(
                        text = "🎉 Поздравляем!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Вы собрали головоломку ${gameState.settings.size}×${gameState.settings.size}!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    // Results Block
                    if (gameState.settings.mode == GameMode.COMPETITION) {
                        CompetitionResultsBlock(gameState)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WinStatItem(Icons.Default.Timer, "Время", formatWinTime(gameState.timeSeconds))
                            WinStatItem(Icons.Default.Gesture, "Ходы", gameState.moves.toString())
                        }
                    }

                    // Awarded Card Section
                    gameState.newCard?.let { card ->
                        val rankColor = when (card.rank) {
                            CardRank.BRONZE -> Color(0xFFCD7F32)
                            CardRank.SILVER -> Color(0xFFC0C0C0)
                            CardRank.GOLD   -> Color(0xFFFFD700)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Вы получили карточку!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Box(
                                modifier = Modifier
                                    .graphicsLayerScaleX(scaleX)
                                    .width(160.dp)
                                    .aspectRatio(535f / 707f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, rankColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            ) {
                                if (showFront) {
                                    CardFrontContent(card = card, context = androidx.compose.ui.platform.LocalContext.current, rankColor = rankColor)
                                } else {
                                    CardBackContent(rankColor = rankColor)
                                }
                            }
                            
                            AnimatedVisibility(
                                visible = showFront,
                                enter = fadeIn() + expandVertically()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    RankStrip(rank = card.rank, rankColor = rankColor, fontSize = 12.sp)
                                    if (card.isDuplicate) {
                                        Text(
                                            "ДУБЛИКАТ",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Record Banner
                    if (gameState.isNewRecord || gameState.ratingChange != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (gameState.ratingChange != null && (gameState.ratingChange ?: 0) > 0) 
                                    Color(0xFF4CAF50).copy(alpha = 0.1f) 
                                    else Color(0xFFFFD700).copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, if (gameState.ratingChange != null) Color(0xFF4CAF50) else Color(0xFFFFD700)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (gameState.ratingChange != null) {
                                    Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF388E3C))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "РЕЙТИНГ: +${gameState.ratingChange} 📈",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF388E3C)
                                    )
                                } else {
                                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFDAA520))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "НОВЫЙ РЕКОРД! 🏆",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB8860B)
                                    )
                                }
                            }
                        }
                    }

                    // Achievements
                    if (gameState.newAchievements.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                            Text("Полученные достижения:", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            gameState.newAchievements.take(3).forEach { achievement ->
                                AchievementItem(achievement)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Buttons Grid
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val buttonModifier = Modifier.weight(1f, fill = false).minimumInteractiveComponentSize()
                        
                        // Play Again
                        Button(
                            onClick = onPlayAgain,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Играть снова", maxLines = 1, softWrap = false)
                        }

                        // Mode Specific: Next Level
                        if (gameState.settings.mode == GameMode.ENDLESS || (gameState.settings.mode == GameMode.TIME_ATTACK && gameState.settings.isSurvivalMode)) {
                            Button(
                                onClick = onNextLevel,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Следующий уровень", maxLines = 1, softWrap = false)
                            }
                        }

                        // Competition: Rematch
                        if (gameState.settings.mode == GameMode.COMPETITION) {
                            Button(
                                onClick = onPlayAgain, // Re-use onPlayAgain which triggers startNewGame with same settings
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                            ) {
                                Icon(Icons.Default.FlashOn, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Реванш", maxLines = 1, softWrap = false)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onNewGame, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Casino, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Новая игра", maxLines = 1, softWrap = false, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = onShowStats, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.BarChart, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Рекорды", maxLines = 1, softWrap = false, fontSize = 12.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Share, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Поделиться", maxLines = 1, softWrap = false, fontSize = 12.sp)
                            }
                            FilledTonalButton(onClick = onMainMenu, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Home, null)
                                Spacer(Modifier.width(4.dp))
                                Text("В меню", maxLines = 1, softWrap = false, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitionResultsBlock(gameState: GameState) {
    val isTeam = gameState.settings.competitionType == CompetitionType.TEAM_2V2
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Участник", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Text("Время", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Text("Ходы", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            HorizontalDivider()
            
            // You
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Вы", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(formatWinTime(gameState.timeSeconds), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(gameState.moves.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            // Team partner (only shown for 2v2 matches)
            if (isTeam) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Groups, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text("Напарник", color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        "${(gameState.teamPartnerProgress * 100).toInt()}%",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("—", modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.secondary)
                }
            }
            
            // Opponent
            val oppName = when {
                isTeam -> "Команда соперника"
                gameState.settings.isAiMatch -> "ИИ (${when(gameState.settings.aiDifficulty) {
                    ChallengeDifficulty.EASY -> "Л"
                    ChallengeDifficulty.MEDIUM -> "С"
                    ChallengeDifficulty.HARD -> "Т"
                    ChallengeDifficulty.EXPERT -> "Э"
                }})"
                else -> "Соперник"
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (isTeam) Icons.Default.Groups else if (gameState.settings.isAiMatch) Icons.Default.SmartToy else Icons.Default.PersonOutline,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(oppName, color = Color.Gray)
                }
                Text(
                    if (gameState.opponentTime != null) formatWinTime(gameState.opponentTime) 
                    else "${(gameState.opponentProgress * 100).toInt()}%",
                    modifier = Modifier.weight(1f), 
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Text(
                    (gameState.opponentMoves ?: 0).toString(), 
                    modifier = Modifier.weight(1f), 
                    textAlign = TextAlign.End,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun WinStatItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AchievementItem(achievement: Achievement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.EmojiEvents, 
            null, 
            tint = when(achievement.rarity) {
                AchievementRarity.GOLD -> Color(0xFFFFD700)
                AchievementRarity.SILVER -> Color(0xFFC0C0C0)
                AchievementRarity.BRONZE -> Color(0xFFCD7F32)
                else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(achievement.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(achievement.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

private fun formatWinTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxItemsInEachRow = maxItemsInEachRow
    ) {
        content()
    }
}
