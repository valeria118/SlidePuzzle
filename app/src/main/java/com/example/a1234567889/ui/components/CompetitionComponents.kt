package com.example.a1234567889.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a1234567889.models.*

@Composable
fun OpponentStatusPanel(
    progress: Float,
    nickname: String?,
    isPublic: Boolean,
    isTeam: Boolean = false,
    partnerProgress: Float = 0f
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isTeam) Icons.Default.Groups else Icons.Default.AccountCircle, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(nickname ?: "Соперник", fontWeight = FontWeight.Bold)
                }
                if (isPublic) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(12.dp), tint = Color.Red)
                            Text(" LIVE", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            CompetitionProgressBar(label = if(isTeam) "Команда соперника" else "Прогресс", progress = progress, color = MaterialTheme.colorScheme.primary)
            
            if (isTeam) {
                Spacer(Modifier.height(8.dp))
                CompetitionProgressBar(label = "Ваш напарник", progress = partnerProgress, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun CompetitionProgressBar(label: String, progress: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun MatchCountdownOverlay(countdown: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = countdown,
            transitionSpec = {
                (scaleIn() + fadeIn()).togetherWith(scaleOut() + fadeOut())
            },
            label = "countdown"
        ) { targetCount ->
            Text(
                text = if (targetCount > 0) targetCount.toString() else "МАРШ!",
                fontSize = 120.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun PowerUpPanel(
    available: List<PowerUpType>,
    active: Map<PowerUpType, Long>,
    onUse: (PowerUpType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PowerUpType.entries.forEach { type ->
            val isAvailable = available.contains(type)
            val isActive = active.containsKey(type)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Color.Yellow 
                            else if (isAvailable) MaterialTheme.colorScheme.secondaryContainer 
                            else Color.Gray.copy(alpha = 0.1f)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isActive) Color(0xFFFFA500) else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable(enabled = isAvailable && !isActive) { onUse(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when(type) {
                            PowerUpType.ACCELERATION -> Icons.Default.FastForward
                            PowerUpType.EXTRA_TIME -> Icons.Default.AddAlarm
                            PowerUpType.AUTO_SOLVE_3 -> Icons.Default.AutoFixHigh
                            PowerUpType.RESET_OPPONENT_MOVES -> Icons.Default.Refresh
                            PowerUpType.FREEZE_OPPONENT -> Icons.Default.AcUnit
                        },
                        contentDescription = null,
                        tint = if (isActive) Color.Black else if (isAvailable) MaterialTheme.colorScheme.onSecondaryContainer else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = when(type) {
                        PowerUpType.ACCELERATION -> "Ускорение"
                        PowerUpType.EXTRA_TIME -> "+Время"
                        PowerUpType.AUTO_SOLVE_3 -> "Авто"
                        PowerUpType.RESET_OPPONENT_MOVES -> "Сброс"
                        PowerUpType.FREEZE_OPPONENT -> "Фриз"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isAvailable) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
        }
    }
}
