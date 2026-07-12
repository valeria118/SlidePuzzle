package com.example.a1234567889.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.a1234567889.data.GamePreferences
import com.example.a1234567889.models.ChallengeMode
import com.example.a1234567889.models.GameMode
import com.example.a1234567889.models.GameSettings

@Composable
fun StatsDialog(
    settings: GameSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = GamePreferences(context)
    val bestTime by prefs.getBestTime(settings.size, settings.mode, settings.challenge).collectAsState(initial = null)
    val bestMoves by prefs.getBestMoves(settings.size, settings.mode, settings.challenge).collectAsState(initial = null)
    val totalGames by prefs.totalGames.collectAsState(initial = 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Статистика") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Текущие настройки: ${settings.size}x${settings.size}, ${settings.mode.name}, ${settings.challenge.name}")
                HorizontalDivider()
                Text("Лучшее время: ${bestTime?.let { formatTime(it) } ?: "Нет данных"}")
                Text("Лучшие ходы: ${bestMoves ?: "Нет данных"}")
                HorizontalDivider()
                Text("Всего партий собрано: $totalGames")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
