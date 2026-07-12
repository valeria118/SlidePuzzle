package com.example.a1234567889.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1234567889.models.*

@Composable
private fun CompetitionResultsLossBlock(gameState: GameState) {
    val isTeam = gameState.settings.competitionType == CompetitionType.TEAM_2V2
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Участник", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Text("Время", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Text("Ходы", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            HorizontalDivider()
            
            // Opponent (Winner)
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
                        if (isTeam) Icons.Default.Groups else if (gameState.settings.isAiMatch) Icons.Default.SmartToy else Icons.Default.Person,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(oppName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                Text(formatTime(gameState.opponentTime ?: gameState.timeSeconds), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text((gameState.opponentMoves ?: 0).toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            // You
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PersonOutline, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("Вы", color = Color.Gray)
                }
                Text(formatTime(gameState.timeSeconds), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.Gray)
                Text(gameState.moves.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = Color.Gray)
            }

            // Team partner (only shown for 2v2 matches)
            if (isTeam) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Groups, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text("Напарник", color = Color.Gray)
                    }
                    Text(
                        "${(gameState.teamPartnerProgress * 100).toInt()}%",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Text("—", modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    gameState: GameState,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    val isEndless = gameState.settings.mode == GameMode.ENDLESS
    val isSurvival = gameState.settings.mode == GameMode.TIME_ATTACK && gameState.settings.isSurvivalMode
    val isCompetition = gameState.settings.mode == GameMode.COMPETITION

    AlertDialog(
        onDismissRequest = { },
        title = { 
            Text(
                if (isEndless) "Марафон завершен!" 
                else if (isSurvival) "Выживание окончено!" 
                else if (isCompetition) "Поражение!"
                else "Время вышло!",
                fontWeight = FontWeight.Bold,
                color = if (isCompetition) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCompetition) {
                    Text(
                        if (gameState.settings.competitionType == CompetitionType.TEAM_2V2)
                            "Команда соперника собрала пазл быстрее вашей команды."
                        else
                            "Соперник собрал пазл быстрее вас.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CompetitionResultsLossBlock(gameState)
                } else if (isEndless || isSurvival) {
                    Text("Вы отлично справились! Вот ваши результаты:", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val currentLevel = gameState.currentLevel
                    
                    ResultRow("Пройдено уровней:", (currentLevel - 1).toString())
                    
                    if (isEndless) {
                        val cycles = (currentLevel - 1) / 5
                        if (cycles > 0) {
                            ResultRow("Завершено циклов:", cycles.toString())
                        }
                        ResultRow("Всего очков:", gameState.score.toString())
                        ResultRow("Общее время:", formatTime(gameState.totalEndlessTime))
                        ResultRow("Всего ходов:", gameState.totalEndlessMoves.toString())
                    } else if (isSurvival) {
                        ResultRow("Достигнутый размер:", "${gameState.settings.size}x${gameState.settings.size}")
                        ResultRow("Общее время:", formatTime(gameState.totalSurvivalTime))
                        ResultRow("Всего ходов:", gameState.totalSurvivalMoves.toString())
                    }
                } else {
                    Text("К сожалению, вы не успели собрать пазл. Попробуйте еще раз!")
                }
            }
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text(
                    text = if (isEndless || isSurvival) "Новый забег" else "Повторить",
                    maxLines = 1,
                    softWrap = false
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("Выйти", maxLines = 1, softWrap = false)
            }
        }
    )
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
