package com.example.a1234567889.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.a1234567889.R

@Composable
fun TopStatsPanel(
    moves: Int,
    seconds: Long,
    timeLeftSeconds: Int? = null,
    movesLeft: Int? = null,
    lives: Int? = null,
    currentLevel: Int? = null,
    progressPercent: Int? = null,
    showCounters: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!showCounters) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                StatItem(label = stringResource(R.string.moves), value = moves.toString())
                if (movesLeft != null) {
                    Text("Осталось: $movesLeft", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                if (currentLevel != null && currentLevel >= 1) {
                    Text("Уровень: $currentLevel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            if (lives != null) {
                val maxLives = 5 // Based on Easy mode being the maximum
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Row 1: up to 3 hearts
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { i ->
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (i < lives) Color.Red else Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    // Row 2: hearts 4 and 5 (if applicable)
                    if (maxLives > 3) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(2) { i ->
                                val heartIndex = i + 3
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (heartIndex < lives) Color.Red else Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (progressPercent != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Прогресс", style = MaterialTheme.typography.labelSmall)
                    CircularProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }

            if (timeLeftSeconds != null) {
                StatItem(
                    label = "Осталось", 
                    value = formatTime(timeLeftSeconds.toLong()),
                    valueColor = if (timeLeftSeconds < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            } else {
                StatItem(label = stringResource(R.string.time), value = formatTime(seconds))
            }
        }
        
        if (progressPercent != null) {
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun BottomControlPanel(
    onShuffle: () -> Unit,
    onPause: () -> Unit,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onShuffle) {
            Text(stringResource(R.string.shuffle), maxLines = 1, softWrap = false)
        }
        IconButton(onClick = onPause) {
            Icon(
                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) stringResource(R.string.resume) else stringResource(R.string.pause)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String, 
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.headlineSmall, 
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
